package com.github.aiwe.materialpopupmenu

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.*
import androidx.appcompat.view.ContextThemeWrapper
import com.github.aiwe.materialpopupmenu.internal.MaterialRecyclerViewPopupWindow
import com.github.aiwe.materialpopupmenu.internal.PopupMenuAdapter
import com.github.aiwe.materialpopupmenu.internal.VisibilityParams

/**
 * Holds all the required information for showing a popup menu.
 *
 * @param style Style of the popup menu. See [MaterialPopupMenuBuilder.style]
 * @param dropdownGravity Gravity of the dropdown list. See [MaterialPopupMenuBuilder.dropdownGravity]
 * @param sections a list of sections
 *
 * @author Piotr Zawadzki
 */
class MaterialPopupMenu
internal constructor(
    @StyleRes internal val style: Int,
    internal val dropdownGravity: Int,
    internal var sections: List<PopupMenuSection>,
    internal val fixedContentWidthInPx: Int,
    internal val dropDownVerticalOffset: Int?,
    internal val dropDownHorizontalOffset: Int?,
    internal val needDrawAnchor: Boolean = false,
    internal val ignoreMaxHeight: Boolean = false,
) {

    private var popupWindow: MaterialRecyclerViewPopupWindow? = null

    private var dismissListener: (() -> Unit)? = null

    private var adapter: PopupMenuAdapter? = null

    /**
     * Shows a popup menu in the UI.
     *
     * This must be called on the UI thread.
     * @param context Context
     * @param anchor view used to anchor the popup
     */
    @UiThread
    fun show(
        context: Context,
        anchor: View,
        additionalViewModel: AdditionalViewModel? = null
    ) {
        val style = resolvePopupStyle(context)
        val styledContext = ContextThemeWrapper(context, style)
        val popupWindow = MaterialRecyclerViewPopupWindow(
            context = styledContext,
            dropDownGravity = dropdownGravity,
            fixedContentWidthInPx = fixedContentWidthInPx,
            needDrawAnchor = needDrawAnchor,
            ignoreMaxHeight = ignoreMaxHeight,
            dropDownVerticalOffset = dropDownVerticalOffset,
            dropDownHorizontalOffset = dropDownHorizontalOffset
        )
        popupWindow.additionalViewModel = additionalViewModel
        adapter = PopupMenuAdapter(sections, popupWindow.hapticFeedbackEnabled) { popupWindow.dismiss() }

        popupWindow.adapter = adapter
        popupWindow.anchorView = anchor

        popupWindow.show()
        this.popupWindow = popupWindow
        setOnDismissListener(this.dismissListener)
    }

    fun updateSections(sections: List<MaterialPopupMenuBuilder.SectionHolder>) {
        this.sections = sections.map { it.convertToPopupMenuSection() }
        val hapticFeedbackEnabled = popupWindow?.hapticFeedbackEnabled ?: false
        adapter = PopupMenuAdapter(this.sections, hapticFeedbackEnabled) { popupWindow?.dismiss() }
        popupWindow?.update(adapter)
    }

    fun setVisibleAnchor(visible: Boolean) {
        popupWindow?.setVisibility(VisibilityParams.ANCHOR, visible)
    }

    fun setVisibleMenu(visible: Boolean) {
        popupWindow?.setVisibility(VisibilityParams.MENU, visible)
    }

    fun setVisibleAdditionalView(visible: Boolean) {
        popupWindow?.setVisibility(VisibilityParams.ADDITIONAL_VIEW, visible)
    }

    fun setTouchOutsideListener(touchOutsideListener: (() -> Unit)?) {
        popupWindow?.touchOutsideListener = touchOutsideListener
    }

    /**
     * Dismisses the popup window.
     */
    @UiThread
    fun dismiss() {
        this.popupWindow?.dismiss()
    }

    /**
     * Sets a listener that is called when this popup window is dismissed.
     *
     * @param listener Listener that is called when this popup window is dismissed.
     */
    fun setOnDismissListener(listener: (() -> Unit)?) {
        this.dismissListener = listener
        this.popupWindow?.setOnDismissListener(listener)
    }

    private fun resolvePopupStyle(context: Context): Int {
        if (style != 0) {
            return style
        }

        val a = context.obtainStyledAttributes(intArrayOf(R.attr.materialPopupMenuStyle))
        val resolvedStyle = a.getResourceId(0, R.style.Widget_MPM_Menu)
        a.recycle()

        return resolvedStyle
    }

    data class AdditionalViewModel(val additionalView: View, val maxHeight: Int)

    internal data class PopupMenuSection(
        val title: CharSequence?,
        val items: List<AbstractPopupMenuItem>
    )

    internal data class PopupMenuItem(
        val label: CharSequence?,
        @StringRes val labelRes: Int,
        @ColorInt val labelColor: Int,
        @DrawableRes val icon: Int,
        val iconDrawable: Drawable?,
        @ColorInt val iconColor: Int,
        val hasNestedItems: Boolean,
        override val viewBoundCallback: ViewBoundCallback,
        override val callback: () -> Unit,
        override val dismissOnSelect: Boolean
    ) : AbstractPopupMenuItem(callback, dismissOnSelect, viewBoundCallback)

    internal data class PopupMenuCustomItem(
        @LayoutRes val layoutResId: Int,
        override val viewBoundCallback: ViewBoundCallback,
        override val callback: () -> Unit,
        override val dismissOnSelect: Boolean
    ) : AbstractPopupMenuItem(callback, dismissOnSelect, viewBoundCallback)

    internal abstract class AbstractPopupMenuItem(
        open val callback: () -> Unit,
        open val dismissOnSelect: Boolean,
        open val viewBoundCallback: ViewBoundCallback
    )
}
