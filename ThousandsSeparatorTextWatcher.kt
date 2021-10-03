
import android.text.Editable
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import java.text.DecimalFormatSymbols
import java.util.Locale

// ThousandsSeparatorTextWatcher.kt --> add this TextWatcher to the EditText you want to add the functionality of dynamic locale aware thousands separator
class ThousandsSeparatorTextWatcher(private var editText: EditText?, private val callback: TextChangedCallback) : TextWatcher {

    //keeping a count of the digits before the cursor to reset the cursor at the correct place
    private var digitsBeforeCursor = -1
    private val thousandSeparator: Char = DecimalFormatSymbols(Locale.getDefault()).groupingSeparator
    private val decimalMarker: Char = DecimalFormatSymbols(Locale.getDefault()).decimalSeparator

    init {
        editText?.apply {

            addTextChangedListener(this@ThousandsSeparatorTextWatcher)

            //disabling copy/paste to avoid format and parse errors
            disableTextSelection(this)

            //diabling text selection
            isLongClickable = false
            setTextIsSelectable(false)

            //ensuring correct input type
            keyListener = DigitsKeyListener.getInstance("0123456789$decimalMarker");
        }
    }

    private fun disableTextSelection(editText: EditText) {

        editText.customSelectionActionModeCallback = object : android.view.ActionMode.Callback {

            override fun onActionItemClicked(mode: android.view.ActionMode?, item: MenuItem?) = false

            override fun onCreateActionMode(mode: android.view.ActionMode?, menu: Menu?) = false

            override fun onPrepareActionMode(mode: android.view.ActionMode?, menu: Menu?) = false

            override fun onDestroyActionMode(mode: android.view.ActionMode?) {}
        }
    }

    /***
     * We are going to calculate the number of numeric digits before the cursor when user starts editing
     * We will keep a count of this number to reset the cursor to the correct position after editing is complete
     */
    override fun beforeTextChanged(sequenceBeforeEdit: CharSequence, startPos: Int, count: Int, after: Int) {

        val textBeforeEdit = sequenceBeforeEdit.toString()

        if (textBeforeEdit.isEmpty()) {
            //in an empty string, cursor position is at 1 if a character is being added (after == 1)
            //if a character is not being added, cursor position remains at the beginning
            digitsBeforeCursor = if (after == 0) -1 else 1
            return
        }

        digitsBeforeCursor = if (after == 0) {
            //if characters are being removed
            //count will always be 1 since we have disabled selection (in which case count will be equal to the number of characters selected)
            val textBeforeNewCursor = textBeforeEdit.substring(0, startPos)
            textBeforeNewCursor.count { it != thousandSeparator }
        } else {
            //if characters are being added
            //after will always be 1 since we have disabled pasting (in which case after will be equal to the number of characters being pasted)
            if (startPos == textBeforeEdit.length) {
                //if adding a character to the end of the string
                textBeforeEdit.count { it != thousandSeparator } + 1
            } else {
                //if adding a character in between the string
                val textBeforeNewCursor = textBeforeEdit.substring(0, startPos + 1)
                textBeforeNewCursor.count { it != thousandSeparator }
            }
        }
    }

    override fun onTextChanged(textAfterEdit: CharSequence, start: Int, before: Int, count: Int) {}

    /***
     * We will get the numeric value in the editText after stripping all the formatting
     * We will then reformat this number to add the correct thousands separation and decimal marker according to the locale
     * We then set the cursor to the correct position as we calculated in beforeTextChanged()
     */
    override fun afterTextChanged(editable: Editable) {

        val text = editable.toString()

        //if the EditText is cleared, trigger callback with a null value to indicate an empty field
        if (text.isEmpty()) {
            digitsBeforeCursor = -1
            callback.onChanged(null)
            return
        }

        //get the double value of the entered number
        val numberValue = getNumberFromFormattedCurrencyText(text)

        //re-format the number to get the correct separation format and symbols
        var newText = getCurrencyFormattedAmountValue(numberValue)

        //If user was inputting decimal part of the number, reformatting will return a string without decimal point.
        //So we need to add it back after the reformatting is complete
        if (text.endsWith(decimalMarker)) {
            newText += decimalMarker
        } else if (text.endsWith(decimalMarker + "0")) {
            newText += decimalMarker + "0"
        }

        //removing the listener to prevent infinite triggers
        editText?.removeTextChangedListener(this)

        //set the reformatted text
        editText?.setText(newText)

        //send the number typed to the callback
        callback.onChanged(numberValue)

        //set the cursor to the right position after reformatting the string
        if (digitsBeforeCursor != -1) {
            var numbersParsed = 0
            for (i in newText.indices) {
                if (newText[i] != thousandSeparator) {
                    numbersParsed++
                }
                if (numbersParsed == digitsBeforeCursor) {
                    editText?.setSelection(i + 1)
                    break
                }
            }
            digitsBeforeCursor = -1
        }

        //add the listener back
        editText?.addTextChangedListener(this)
    }

    /***
     * Function to remove the listener and release reference to the EditText
     */
    fun removeWatcherFromEditText() {
        editText?.removeTextChangedListener(this)
        editText = null
    }

    interface TextChangedCallback {
        fun onChanged(newNumber: Double?)
    }
    
    companion object{
        
        @JvmStatic
        fun getNumberFromFormattedCurrencyText(formattedText: String?) = formattedText?.let {
            val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
            try {
                numberFormat.parse(it)?.toDouble()
            } catch (exception: ParseException) {
                0.0
            }
        } ?: 0.0

        @JvmStatic
        fun getCurrencyFormattedAmountValue(amount: Double?) = amount?.let {
            val numberFormat = NumberFormat.getNumberInstance(Locale.getDefault())
            numberFormat.maximumFractionDigits = 2
            numberFormat.format(amount)
        } ?: ""
    }
}
