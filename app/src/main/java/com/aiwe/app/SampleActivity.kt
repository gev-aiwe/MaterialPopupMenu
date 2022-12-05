package com.aiwe.app

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.RadioButton
import androidx.annotation.StyleRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.aiwe.app.databinding.ActivitySampleBinding
import com.aiwe.app.extensions.roundPixels
import com.aiwe.app.extensions.toPixelFromDip
import com.aiwe.app.persistence.DEFAULT_POPUP_STYLE
import com.aiwe.app.persistence.SelectionRepository
import com.aiwe.app.persistence.SelectionRepositoryImpl
import com.github.aiwe.materialpopupmenu.MaterialPopupMenuBuilder
import com.google.android.material.radiobutton.MaterialRadioButton

class SampleActivity : AppCompatActivity() {

    private val selectionRepository: SelectionRepository by lazy { SelectionRepositoryImpl(applicationContext) }
    private lateinit var binding: ActivitySampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        val themeToUse = selectionRepository.theme
        setTheme(themeToUse)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        initializeThemeButtonGroup(themeToUse)
        initializePopupStyleButtonGroup()
        initializeGravityButtonGroup()
        initializePopupRadioGroup()
        initializeShowPopupButton()
        initializeAnchorViewWidthInputEditText()
    }

    private fun initializeGravityButtonGroup() = with(binding) {
        selectionRepository.dropdownGravities
            .map { GRAVITY_TO_GRAVITY_BUTTON_ID_MAP.getValue(it) }
            .forEach { gravityToggleGroup.check(it) }
        gravityToggleGroup.addOnButtonCheckedListener { group, _, _ ->
            selectionRepository.dropdownGravities = group.checkedButtonIds.map { GRAVITY_BUTTON_ID_TO_GRAVITY_MAP.getValue(it) }
        }
    }

    private fun initializeAnchorViewWidthInputEditText() = with(binding) {
        anchorWidthInputEditText.addTextChangedListener(afterTextChanged = { editable ->
            if (editable.isNullOrEmpty()) return@addTextChangedListener

            editable.toString().toInt().let {
                selectionRepository.anchorWidthInDp = it
                showPopupButton.layoutParams.width = this@SampleActivity.toPixelFromDip(it).roundPixels()
                showPopupButton.requestLayout()
            }
        })
        anchorWidthInputEditText.setText(selectionRepository.anchorWidthInDp.toString())
    }

    private fun initializeShowPopupButton() = with(binding) {
        container.addDraggableChild(showPopupButton)
        showPopupButton.setOnClickListener(::showPopupMenu)
    }

    private fun showPopupMenu(clickedView: View) = with(binding) {
        val checkedSamplePosition = selectionRepository.samplePosition
        val materialPopupMenuBuilder = SAMPLES[checkedSamplePosition].popupMenuProvider(this@SampleActivity, clickedView)
        val popupMenuStyle = selectionRepository.popupStyle
        if (isCustomPopupMenuStyleUsed(popupMenuStyle) && canOverrideDefaultPopupStyle(materialPopupMenuBuilder)) {
            materialPopupMenuBuilder.style = popupMenuStyle
        }
        var jointGravity = 0
        selectionRepository.dropdownGravities.forEach { gravity -> jointGravity = jointGravity or gravity }
        if (jointGravity != 0) {
            materialPopupMenuBuilder.dropdownGravity = jointGravity
        }
        materialPopupMenuBuilder.build().run {
            show(this@SampleActivity, clickedView)
            setOnDismissListener { Log.i(TAG, "Popup dismissed!") }
        }
    }

    private fun isCustomPopupMenuStyleUsed(popupMenuStyle: Int) = popupMenuStyle != DEFAULT_POPUP_STYLE

    private fun canOverrideDefaultPopupStyle(materialPopupMenuBuilder: MaterialPopupMenuBuilder) = materialPopupMenuBuilder.style == 0

    private fun initializeThemeButtonGroup(themeToUse: Int) = with(binding) {
        val themeButtonIdToCheck = THEME_ID_TO_THEME_BUTTON_ID_MAP.getValue(themeToUse)
        themeToggleGroup.check(themeButtonIdToCheck)
        // TODO this can be simplified once https://github.com/material-components/material-components-android/commit/d4a17635b1a058a88ca03985a83c3990ba6626b7 is released
        listOf(themeDarkButton, themeLightButton).forEach { button ->
            button.setOnClickListener {
                val checkedButtonId = themeToggleGroup.checkedButtonId
                if (checkedButtonId == 0 || checkedButtonId == View.NO_ID) {
                    // Make sure one theme is always checked.
                    themeToggleGroup.check(button.id)
                } else {
                    changeThemeTo(THEME_BUTTON_ID_TO_THEME_ID_MAP.getValue(button.id))
                }
            }
        }
    }

    private fun initializePopupStyleButtonGroup() = with(binding) {
        popupStyleToggleGroup.check(POPUP_STYLE_ID_TO_POPUP_STYLE_BUTTON_ID_MAP.getValue(selectionRepository.popupStyle))
        popupStyleToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectionRepository.popupStyle = POPUP_STYLE_BUTTON_ID_TO_POPUP_STYLE_ID_MAP.getValue(checkedId)
            }
        }
        // TODO this can be simplified once https://github.com/material-components/material-components-android/commit/d4a17635b1a058a88ca03985a83c3990ba6626b7 is released
        listOf(popupStyleDefaultButton, popupStyleLightButton, popupStyleDarkButton, popupStyleColoredButton).forEach { button ->
            button.setOnClickListener {
                val checkedButtonId = popupStyleToggleGroup.checkedButtonId
                if (checkedButtonId == 0 || checkedButtonId == View.NO_ID) {
                    // Make sure one theme is always checked.
                    popupStyleToggleGroup.check(button.id)
                }
            }
        }
    }

    private fun initializePopupRadioGroup() = with(binding) {
        SAMPLES.forEachIndexed { index, item ->
            val radioButton = createRadioButton(index, item)
            popupTypeRadioGroup.addView(radioButton)
        }
        popupTypeRadioGroup.check(selectionRepository.samplePosition)
        popupTypeRadioGroup.setOnCheckedChangeListener { group, _ ->
            val checkedRadioButton = group.findViewById<RadioButton>(popupTypeRadioGroup.checkedRadioButtonId)
            selectionRepository.samplePosition = popupTypeRadioGroup.indexOfChild(checkedRadioButton)
        }
    }

    private fun createRadioButton(index: Int, sample: PopupMenuSample): MaterialRadioButton = with(binding) {
        MaterialRadioButton(popupTypeRadioGroup.context).apply {
            id = index
            setText(sample.label)
        }
    }

    private fun changeThemeTo(@StyleRes newThemeStyle: Int) {
        selectionRepository.theme = newThemeStyle
        val intent = Intent(this, SampleActivity::class.java)
        val activityOptions = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out)
        startActivity(intent, activityOptions.toBundle())
        finish()
    }
}

private const val TAG = "MPM"

private val THEME_BUTTON_ID_TO_THEME_ID_MAP: Map<Int, Int> = mapOf(
    R.id.themeDarkButton to R.style.AppTheme_Dark,
    R.id.themeLightButton to R.style.AppTheme_Light
)

private val THEME_ID_TO_THEME_BUTTON_ID_MAP: Map<Int, Int> = mapOf(
    R.style.AppTheme_Dark to R.id.themeDarkButton,
    R.style.AppTheme_Light to R.id.themeLightButton
)

private val POPUP_STYLE_BUTTON_ID_TO_POPUP_STYLE_ID_MAP: Map<Int, Int> = mapOf(
    R.id.popupStyleDefaultButton to DEFAULT_POPUP_STYLE,
    R.id.popupStyleLightButton to R.style.Widget_MPM_Menu,
    R.id.popupStyleDarkButton to R.style.Widget_MPM_Menu_Dark,
    R.id.popupStyleColoredButton to R.style.Widget_MPM_Menu_Dark_ColoredBackground
)

private val POPUP_STYLE_ID_TO_POPUP_STYLE_BUTTON_ID_MAP: Map<Int, Int> = mapOf(
    DEFAULT_POPUP_STYLE to R.id.popupStyleDefaultButton,
    R.style.Widget_MPM_Menu to R.id.popupStyleLightButton,
    R.style.Widget_MPM_Menu_Dark to R.id.popupStyleDarkButton,
    R.style.Widget_MPM_Menu_Dark_ColoredBackground to R.id.popupStyleColoredButton
)

private val GRAVITY_BUTTON_ID_TO_GRAVITY_MAP: Map<Int, Int> = mapOf(
    R.id.gravityStartButton to Gravity.START,
    R.id.gravityEndButton to Gravity.END,
    R.id.gravityBottomButton to Gravity.BOTTOM,
    R.id.gravityTopButton to Gravity.TOP
)

private val GRAVITY_TO_GRAVITY_BUTTON_ID_MAP: Map<Int, Int> = mapOf(
    Gravity.START to R.id.gravityStartButton,
    Gravity.END to R.id.gravityEndButton,
    Gravity.BOTTOM to R.id.gravityBottomButton,
    Gravity.TOP to R.id.gravityTopButton
)
