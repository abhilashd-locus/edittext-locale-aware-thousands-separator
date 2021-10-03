# edittext-locale-aware-thousands-separator
Adding locale aware thousands separator dynamically as the user types

Features:
* Add thousands separator dynamically as the user types
* Enable editing in between the string and not only at the ends
* Style of thousands separation is based upon the locale (eg: 100,000 vs 1,00,000)
* Symbol of thousands separator and decimal marker is based on the locale (eg: 100,000.00 vs 100.000,00)
* Supports all languages and locales 

Disadvantages:
* Does not support copy/paste operations
* In left-to-right languages (eg. Arabic), the cursor jumps to the end on deleting the first number

Note:
* If you are programmatically setting text, always set formatted text
