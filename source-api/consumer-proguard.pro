-keep class com.shinku.reader.source.model.** { public protected *; }
-keep class com.shinku.reader.source.online.** { public protected *; }
-keep class com.shinku.reader.source.** extends com.shinku.reader.source.Source { public protected *; }

-keep,allowoptimization class com.shinku.reader.util.JsoupExtensionsKt { public protected *; }

-keep class com.shinku.reader.exh.metadata.** { public protected *; }