package app.muster.data.platform

// Must match a platform in get_feature_flags. Any other value gets the web flags.
internal expect val platformName: String
