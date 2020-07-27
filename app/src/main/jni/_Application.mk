
LOCAL_IS_SUPPORT_STL := false
ifeq ($(LOCAL_IS_SUPPORT_STL),true)
	APP_STL := stlport_static
	#STLPORT_FORCE_REBUILD := true
endif

