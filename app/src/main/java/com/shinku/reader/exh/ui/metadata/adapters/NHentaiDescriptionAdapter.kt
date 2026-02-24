package com.shinku.reader.exh.ui.metadata.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.shinku.reader.R
import com.shinku.reader.databinding.DescriptionAdapterNhBinding
import com.shinku.reader.ui.manga.MangaScreenModel.State
import com.shinku.reader.util.system.copyToClipboard
import com.shinku.reader.exh.metadata.MetadataUtil
import com.shinku.reader.exh.metadata.metadata.NHentaiSearchMetadata
import com.shinku.reader.exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable
import com.shinku.reader.core.common.i18n.pluralStringResource
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.i18n.MR
import com.shinku.reader.i18n.sy.SYMR
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
fun NHentaiDescription(state: State.Success, openMetadataViewer: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { factoryContext ->
            DescriptionAdapterNhBinding.inflate(LayoutInflater.from(factoryContext)).root
        },
        update = {
            val meta = state.meta
            if (meta == null || meta !is NHentaiSearchMetadata) return@AndroidView
            val binding = DescriptionAdapterNhBinding.bind(it)

            binding.genre.text = meta.tags.filter {
                it.namespace == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE
            }.let { tags ->
                if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
            }.let { categoriesString ->
                categoriesString?.let { MetadataUIUtil.getGenreAndColour(context, it) }?.let {
                    binding.genre.setBackgroundColor(it.first)
                    it.second
                } ?: categoriesString ?: context.stringResource(MR.strings.unknown)
            }

            meta.favoritesCount?.let {
                if (it == 0L) return@let
                binding.favorites.text = it.toString()
                binding.favorites.bindDrawable(context, R.drawable.ic_book_24dp)
            }

            binding.whenPosted.text = MetadataUtil.EX_DATE_FORMAT
                .format(
                    ZonedDateTime
                        .ofInstant(Instant.ofEpochSecond(meta.uploadDate ?: 0), ZoneId.systemDefault()),
                )

            binding.pages.text = context.pluralStringResource(
                SYMR.plurals.num_pages,
                meta.pageImageTypes.size,
                meta.pageImageTypes.size,
            )
            binding.pages.bindDrawable(context, R.drawable.ic_baseline_menu_book_24)

            @SuppressLint("SetTextI18n")
            binding.id.text = "#" + (meta.nhId ?: 0)

            binding.moreInfo.bindDrawable(context, R.drawable.ic_info_24dp)

            listOf(
                binding.favorites,
                binding.genre,
                binding.id,
                binding.pages,
                binding.whenPosted,
            ).forEach { textView ->
                textView.setOnLongClickListener {
                    context.copyToClipboard(
                        textView.text.toString(),
                        textView.text.toString(),
                    )
                    true
                }
            }

            binding.moreInfo.setOnClickListener {
                openMetadataViewer()
            }
        },
    )
}
