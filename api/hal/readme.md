# HAL 层分析
> HAL 层又叫硬件抽象层.位于内核和Android之间。Android是否开源的秘密就是再这层.Google将硬件厂商驱动放在这层，没有开源才被Linux删除。

## 1.认识HAL层
HAL_Legacy是过去的HAL模块。现在使用的是HAL。

`HAL`采用HAL_module和HAL stub结合形式。上层拥有访问HAL stub函数指针，并不需要整个HAL stub.
`HAL stub`模式是一种代理模式。虽然stub仍然是*.so文件形式，但是HAL层将*.so文件隐藏了。stub在HAL层向上层提供操作函数,Runtime只需要说明module ID就可以取得操作函数。

## 2.分析HAL层代码
对应不同hardware的HAL,对应的lib命名规则是id.variant.so；如gralloc.msm7k.so，variant对应在variant_keys对应的值.在HAL module主要三个结构体:

* `struct hw_device_t`
* `struct hw_module_t`
* `struct hw_module_methods_t`

三个结构体关系：
``` c
typedef struct hw_device_t {
    ...
    struct hw_module_t*module;
    ...
}hw_device_t;

typedef struct hw_module_t {
    ...
    struct hw_module_methods_t*methods;
    ...
} hw_module_t;
typedef struct hw_module_methods_t{
    int(*open)(const struct hw_module_t*module ,const char*id,struct hw_device_t**device);
} hw_module_methods_t;
```
### (1)函数`hw_get_module()`
此函数功能:根据模块ID寻找硬件模块动态链接库的地址，然后调用load打开动态链接库,根据`HAL_MODULE_INFO_SYM`并获取硬件模块结构体。在hw_module_t提供结构体函数打开相应模块，并初始化。

`open()`会传入`hw_device_t`指针，对应的模块保存在该指针里面，通过它进行交互.
```c
//hardware/libhardware/hardware.c
#define HAL_LIBRARY_PATH1 "/system/lib/hw"
#define HAL_LIBRARY_PATH2 "/vendor/lib/hw"
static const char*variant_keys[]={
    "ro.hardware",
    "ro.product.board",
    "ro.arch"
};
static const int HAL_VARIANT_KEYS_COUNT=(sizeof(variant_keys/sizeof(variant_keys[0])));

int hw_get_module(const char*class_id,const struct hw_module_t**module){
    int status;
    int i;
    const struct hw_module_t*hmi=NULL;
    char prop[PATH_MAX];
    char path[PATH_MAX];
    char name[PATH_MAX];
    strlcpy(name,id,PATH_MAX);
    //同时多次调用时候，只是增加引用，不会新加载库.假设dlopen()线程安全
    for(i=0;i<HAL_VARIANT_KEYS_COUNT+1;i++){//通过配置variants找模块
        if(i<HAL_VARIANT_KEYS_COUNT){
            if(property_get(variant_keys[i],prop,NULL)==0){
                continue;
            }
            snprintf(path,sizeof(path),"%s/%s.%s.so"
            HAL_LIBRARY_PATH2,name,prop);
            if(access(path,R_OK)) break; 
            snprintf(path,sizeof(path),"%s/%s.%s.so"
            HAL_LIBRARY_PATH,name,prop);
            if(access(path,R_OK)) break; 
        }else{
            snprintf(path,sizeof(path),"%s/%s.default.so",HAL_LIBRARY_PATH2,name);
            if(access(path,R_OK)) break; 
            snprintf(path,sizeof(path),"%s/%s.default.so",HAL_LIBRARY_PATH,name);
            if(access(path,R_OK)) break; 

        }
    }
    status=-ENOENT;
    if(i<HAL_VARIENT_COUNT+1){
        //如果失败，不会加载其他不同的variant
        status=load(class_id,path,module);
    }
    return status;
}
static int load(const char *id,const char* path,const struct hw_module_t**pHmi){
    int status;
    void*handle;
    struct hw_module_t *hmi;
    handle=dlopen(path,RTLD_NOW);
    //获取hal_module_info地址
    const char*sym=HAL_MODULE_INFO_SYM_AS_STR;
    hmi=(struct hw_module_t*)dlsym(handle,sym);
    //检查id匹配
    strcmp(id,hmi->id);
    hmi->dso=handle;
    dlclose(handle);
    *pHmi=hmi;
    return status;
}
```
## 3.例子展示

### (1) service实现
    app可直接通过service调用.so格式的JNI。高效但是不正规
### (2) manager实现
    Manager调用Service：复杂，符合Android 框架
    
## 4.设备权限
### (1)设置设备权限
> Android系统启动时，内核引导参数上一般设置init=/init，**挂载** 了这个文件系统，首先运行根目录Init程序.

新增驱动程序的设备节点，需要随之更改这些设备节点属性。
``` c++
//定义.`/system/core/init/devices.c`
struct perms_ {
    char*name;
    char*attr;
    mode_t perm;
    unsigned int uid;
    unsigned int gid;
    usnsigned short prefix;
};
//配置 /system/core/include/private/android_filesystem_config.h
//文件系统一些配置
#define AID_ROOT 0
#define AID_SYSTEM 1000
#define AID_SHELL  2000  //adb and debug shell user
#define AID_CACHE  2001 //chache access
#define AID_APP    10000 // first app user


```


