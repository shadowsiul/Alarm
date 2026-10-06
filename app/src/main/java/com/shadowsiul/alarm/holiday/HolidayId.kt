package com.shadowsiul.alarm.holiday

import androidx.annotation.StringRes
import com.shadowsiul.alarm.R

enum class HolidayId(@StringRes val titleRes: Int) {
    NEW_YEAR(R.string.holiday_new_year),
    MLK(R.string.holiday_mlk),
    WASHINGTON(R.string.holiday_washington),
    MEMORIAL(R.string.holiday_memorial),
    JUNETEENTH(R.string.holiday_juneteenth),
    INDEPENDENCE(R.string.holiday_independence),
    LABOR(R.string.holiday_labor),
    COLUMBUS(R.string.holiday_columbus),
    VETERANS(R.string.holiday_veterans),
    THANKSGIVING(R.string.holiday_thanksgiving),
    CHRISTMAS(R.string.holiday_christmas),
}
