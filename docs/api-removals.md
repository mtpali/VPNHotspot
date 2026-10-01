# Removed feature API documentation

The exact descriptors below were checked against `../hiddenapi/hiddenapi-flags.csv`
(SHA-256 `9102af02fe6ab68b92464bdff5e5b09f3bd62c65d1130aaf85d3296f17d38074`).
The source paths using them were removed with repeater/temporary hotspot support and telemetry cleanup.
No flags are inferred for descriptors absent from that CSV.

| Removed descriptor | Exact CSV flags |
| --- | --- |
| `Landroid/net/wifi/IWifiManager;->registerLocalOnlyHotspotSoftApCallback(Landroid/net/wifi/ISoftApCallback;Landroid/os/Bundle;)V` | `blocked` |
| `Landroid/net/wifi/IWifiManager;->unregisterLocalOnlyHotspotSoftApCallback(Landroid/net/wifi/ISoftApCallback;Landroid/os/Bundle;)V` | `blocked` |
| `Landroid/net/wifi/WifiManager;->cancelLocalOnlyHotspotRequest()V` | `unsupported` |
| `Landroid/net/wifi/p2p/WifiP2pConfig$Builder;->MAC_ANY_ADDRESS:Landroid/net/MacAddress;` | `blocked` |
| `Landroid/net/wifi/p2p/WifiP2pConfig$Builder;->mNetworkName:Ljava/lang/String;` | `blocked` |
| `Landroid/net/wifi/p2p/WifiP2pGroup;->interfaceAddress:[B` | `unsupported` |
| `Landroid/net/wifi/p2p/WifiP2pManager;->startWps(Landroid/net/wifi/p2p/WifiP2pManager$Channel;Landroid/net/wifi/WpsInfo;Landroid/net/wifi/p2p/WifiP2pManager$ActionListener;)V` | `unsupported` |
| `Landroid/hardware/wifi/supplicant/V1_0/IfaceType;->P2P:I` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant;->getService()Landroid/hardware/wifi/supplicant/V1_0/ISupplicant;` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant;->getInterface(Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$IfaceInfo;Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$getInterfaceCallback;)V` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$getInterfaceCallback;->onValues(Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatus;Landroid/hardware/wifi/supplicant/V1_0/ISupplicantIface;)V` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant;->listInterfaces(Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$listInterfacesCallback;)V` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$IfaceInfo;->name:Ljava/lang/String;` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$IfaceInfo;->type:I` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicant$listInterfacesCallback;->onValues(Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatus;Ljava/util/ArrayList;)V` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/ISupplicantP2pIface;->asInterface(Landroid/os/IHwBinder;)Landroid/hardware/wifi/supplicant/V1_0/ISupplicantP2pIface;` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatusCode;->FAILURE_ARGS_INVALID:I` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatus;->code:I` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatus;->debugMessage:Ljava/lang/String;` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_2/ISupplicantP2pIface;->addGroup_1_2(Ljava/util/ArrayList;Ljava/lang/String;ZI[BZ)Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatus;` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_2/ISupplicantP2pIface;->castFrom(Landroid/os/IHwInterface;)Landroid/hardware/wifi/supplicant/V1_2/ISupplicantP2pIface;` | Absent from provided CSV |
| `Landroid/hardware/wifi/supplicant/V1_2/ISupplicantP2pIface;->setMacRandomization(Z)Landroid/hardware/wifi/supplicant/V1_0/SupplicantStatus;` | Absent from provided CSV |
| `Landroid/os/ServiceManager;->waitForDeclaredService(Ljava/lang/String;)Landroid/os/IBinder;` | `blocked` |
| `Landroid/os/ServiceManager;->waitForService(Ljava/lang/String;)Landroid/os/IBinder;` | `blocked` |
| `Lcom/android/server/wifi/SupplicantStaIfaceHalAidlMainlineImpl;->isServiceAvailable(Landroid/content/Context;)Z` | Absent from provided CSV |
| `Landroid/system/wifi/mainline_supplicant/IMainlineSupplicant$Stub;->asInterface(Landroid/os/IBinder;)Landroid/system/wifi/mainline_supplicant/IMainlineSupplicant;` | Absent from provided CSV |
| `Landroid/system/wifi/mainline_supplicant/IMainlineSupplicant;->getVendorSupplicant()Lcom/android/wifi/x/android/hardware/wifi/supplicant/ISupplicant;` | Absent from provided CSV |
| `Lcom/android/server/wifi/p2p/SupplicantP2pIfaceHalAidlBase;->HAL_INSTANCE_NAME:Ljava/lang/String;` | Absent from provided CSV |
| `Lcom/android/server/wifi/p2p/SupplicantP2pIfaceHalAidlMainlineImpl;->MAINLINE_SUPPLICANT_SERVICE_NAME:Ljava/lang/String;` | Absent from provided CSV |
| `Ldalvik/system/BaseDexClassLoader;->pathList:Ldalvik/system/DexPathList;` | `unsupported` |
| `Ldalvik/system/DexPathList;->nativeLibraryDirectories:Ljava/util/List;` | `unsupported` |
| `Landroid/net/TetheringManager;->TETHERING_WIFI_P2P:I` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/WifiManager;->registerLocalOnlyHotspotSoftApCallback(Ljava/util/concurrent/Executor;Landroid/net/wifi/WifiManager$SoftApCallback;)V` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/WifiManager;->startLocalOnlyHotspot(Landroid/net/wifi/SoftApConfiguration;Ljava/util/concurrent/Executor;Landroid/net/wifi/WifiManager$LocalOnlyHotspotCallback;)V` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/WifiManager;->unregisterLocalOnlyHotspotSoftApCallback(Landroid/net/wifi/WifiManager$SoftApCallback;)V` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/p2p/WifiP2pGroup;->getVendorData()Ljava/util/List;` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/p2p/WifiP2pGroupList;->getGroupList()Ljava/util/List;` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/p2p/WifiP2pManager;->requestPersistentGroupInfo(Landroid/net/wifi/p2p/WifiP2pManager$Channel;Landroid/net/wifi/p2p/WifiP2pManager$PersistentGroupInfoListener;)V` | `sdk,system-api,test-api` |
| `Landroid/net/TetheringManager$TetheringEventCallback;->onLocalOnlyInterfacesChanged(Ljava/util/Set;)V` | `sdk,system-api,test-api` |
| `Landroid/os/SystemProperties;->get(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;` | `sdk,system-api,test-api` |
| `Landroid/os/ServiceSpecificException;->errorCode:I` | `sdk,system-api,test-api` |
| `Landroid/os/ServiceSpecificException;-><init>(I)V` | `sdk,system-api,test-api` |
| `Landroid/net/ConnectivityManager;->EXTRA_ACTIVE_LOCAL_ONLY:Ljava/lang/String;` | `lo-prio,max-target-o` |
| `Landroid/net/TetheringManager;->EXTRA_ACTIVE_LOCAL_ONLY:Ljava/lang/String;` | `sdk,system-api,test-api` |
| `Landroid/net/wifi/WifiManager;->IFACE_IP_MODE_LOCAL_ONLY:I` | `sdk,system-api,test-api` |
