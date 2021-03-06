/*
 * MIT License
 *
 * Copyright (c) 2019 SmashKs
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package taiwan.no.one.currency.data.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import taiwan.no.one.currency.domain.model.CountryModel
import java.util.Date

@Entity(tableName = "table_country")
data class CountryData(
    val alpha3: String? = "",
    @ColumnInfo(name = "currency_id")
    val currencyId: String? = "",
    @ColumnInfo(name = "currency_name")
    val currencyName: String? = "",
    @ColumnInfo(name = "currency_symbol")
    val currencySymbol: String? = "",
    @PrimaryKey
    val id: String = "",
    val name: String? = "",
    val updated: Date? = Date(),
) {
    fun convert() = CountryModel(
        name.orEmpty(),
        id.orEmpty(),
        CurrencyData(currencyName, currencySymbol, currencyId).convert()
    )
}
