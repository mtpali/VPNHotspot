use std::ffi::CStr;
use std::io;
use std::sync::OnceLock;

// Set once from Android's Build.VERSION.SDK_INT in the root launcher. The exported NDK
// android_get_device_api_level symbol is absent before API 29; no newer ELF import is needed.
pub(crate) static ANDROID_API_LEVEL: OnceLock<i32> = OnceLock::new();

pub(crate) fn android_api_level() -> i32 {
    *ANDROID_API_LEVEL
        .get()
        .expect("daemon API level initialized before control startup")
}

pub(crate) fn kernel_release() -> io::Result<String> {
    let mut uts = std::mem::MaybeUninit::<libc::utsname>::zeroed();
    if unsafe { libc::uname(uts.as_mut_ptr()) } != 0 {
        return Err(io::Error::last_os_error());
    }
    let uts = unsafe { uts.assume_init() };
    let release = unsafe { CStr::from_ptr(uts.release.as_ptr()) };
    Ok(release.to_string_lossy().into_owned())
}
