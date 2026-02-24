package com.shinku.reader.exh.ui.metadata.adapters

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.shinku.reader.R
import com.shinku.reader.databinding.DescriptionAdapter8mBinding
import com.shinku.reader.ui.manga.MangaScreenModel.State
import com.shinku.reader.util.system.copyToClipboard
import com.shinku.reader.exh.metadata.metadata.EightMusesSearchMetadata
import com.shinku.reader.exh.ui.metadata.adapters.MetadataUIUtil.bindDrawable
import com.shinku.reader.core.common.i18n.stringResource
import com.shinku.reader.i18n.MR

@Composable
fun EightMusesDescription(state: State.Success, openMetadataViewer: () -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { factoryContext ->
            DescriptionAdapter8mBinding.inflate(LayoutInflater.from(factoryContext)).root
        },
        update = {
            val meta = state.meta
            if (meta == null || meta !is EightMusesSearchMetadata) return@AndroidView
            val binding = DescriptionAdapter8mBinding.bind(it)

            binding.title.text = meta.title ?: context.stringResource(MR.strings.unknown)

            binding.moreInfo.bindDrawable(context, R.drawable.ic_info_24dp)

            binding.title.setOnLongClickListener {
                context.copyToClipboard(
                    binding.title.text.toString(),
                    binding.title.text.toString(),
                )
                true
            }

            binding.moreInfo.setOnClickListener {
                openMetadataViewer()
            }
        },
    )
}
