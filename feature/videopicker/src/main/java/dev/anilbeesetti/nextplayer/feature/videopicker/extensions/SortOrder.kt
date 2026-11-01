package dev.anilbeesetti.nextplayer.feature.videopicker.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.anilbeesetti.nextplayer.core.model.Sort
import dev.anilbeesetti.nextplayer.core.ui.R

@Composable
fun Sort.Order.name(sortBy: Sort.By): String {
    val stringRes = when (sortBy) {
        Sort.By.TITLE,
        Sort.By.PATH,
        Sort.By.TYPE,
        -> when (this) {
            Sort.Order.ASCENDING -> R.string.a_z
            Sort.Order.DESCENDING -> R.string.z_a
        }

        Sort.By.LENGTH -> when (this) {
            Sort.Order.ASCENDING -> R.string.shortest
            Sort.Order.DESCENDING -> R.string.longest
        }

        Sort.By.SIZE, Sort.By.RESOLUTION -> when (this) {
            Sort.Order.ASCENDING -> R.string.smallest
            Sort.Order.DESCENDING -> R.string.largest
        }

        Sort.By.DATE, Sort.By.PLAYED -> when (this) {
            Sort.Order.ASCENDING -> R.string.oldest
            Sort.Order.DESCENDING -> R.string.newest
        }
    }

    return stringResource(id = stringRes)
}
