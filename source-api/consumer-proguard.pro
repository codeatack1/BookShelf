-keep class com.bookshelf.source.model.** { public protected *; }
-keep class com.bookshelf.source.online.** { public protected *; }
-keep class com.bookshelf.source.** extends com.bookshelf.source.Source { public protected *; }

-keep,allowoptimization class com.bookshelf.util.JsoupExtensionsKt { public protected *; }
