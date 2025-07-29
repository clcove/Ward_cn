package dev.leons.ward.services;

import dev.leons.ward.Ward;
import dev.leons.ward.dto.*;
import dev.leons.ward.components.UtilitiesComponent;
import dev.leons.ward.exceptions.ApplicationNotConfiguredException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.ExecutingCommand;
import oshi.util.FileUtil;
import oshi.util.Util;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * InfoService provides various information about machine, such as processor name, core count, Ram amount, etc.
 *
 * @author Rudolf Barbu
 * @version 1.0.2
 */
@Service
public class InfoService
{
    /**
     * Autowired SystemInfo object
     * Used for getting machine information
     */
    @Autowired
    private SystemInfo systemInfo;

    /**
     * Autowired UtilitiesComponent object
     * Used for various utility functions
     */
    @Autowired
    private UtilitiesComponent utilitiesComponent;

    /**
     * Converts frequency to most readable format
     *
     * @param hertzArray raw frequency array values in hertz for each logical processor
     * @return String with formatted frequency and postfix
     */
    private String getConvertedFrequency(final long[] hertzArray)
    {
        long totalFrequency = Arrays.stream(hertzArray).sum();
        long hertz = totalFrequency / hertzArray.length;

        if ((hertz / 1E+6) > 999)
        {
            return (Math.round((hertz / 1E+9) * 10.0) / 10.0) + " GHz";
        }
        else
        {
            return Math.round(hertz / 1E+6) + " MHz";
        }
    }

    /**
     * Converts capacity to most readable format
     *
     * @param bits raw capacity value in bits
     * @return String with formatted capacity and postfix
     */
    private String getConvertedCapacity(final long bits)
    {
        if ((bits / 1.049E+6) > 999)
        {
            if ((bits / 1.074E+9) > 999)
            {
                return (Math.round((bits / 1.1E+12) * 10.0) / 10.0) + " TiB";
            }
            else
            {
                return Math.round(bits / 1.074E+9) + " GiB";
            }
        }
        else
        {
            return Math.round(bits / 1.049E+6) + " MiB";
        }
    }

    /**
     * 读取cpu信息
     *
     * @return ProcessorDto with filled fields
     */
    private ProcessorDto getProcessor(HardwareAbstractionLayer hardware) {
        ProcessorDto processorDto = new ProcessorDto();
        //cpu 信息
        CentralProcessor centralProcessor = hardware.getProcessor();
        //传感器信息
        Sensors sensors = hardware.getSensors();
        // cpu型号
        String name = centralProcessor.getProcessorIdentifier().getName().split("@")[0].trim();
        processorDto.setName(name);

        // 核心数
        int coreCount = centralProcessor.getPhysicalProcessorCount();

        //线程数
        int threads = centralProcessor.getLogicalProcessorCount();
        processorDto.setCoreCount(coreCount+"c/"+threads+"t");

        // cpu频率
        processorDto.setClockSpeed(getConvertedFrequency(centralProcessor.getCurrentFreq()));

        // cpu使用率
        processorDto.setUsage(getProcessorUsage(hardware));

        //cpu温度
        processorDto.setTemp(Math.round(sensors.getCpuTemperature()) + "°C");
        return processorDto;
    }

    /**
     * 读取内存信息
     *
     * @return MachineDto with filled fields
     */
    private MachineDto getMachine(HardwareAbstractionLayer hardware) {
        MachineDto machineDto = new MachineDto();

        //内存信息
        GlobalMemory globalMemory = hardware.getMemory();

        //内存总大小
        long totalRam = globalMemory.getTotal();
        machineDto.setTotalRam(getConvertedCapacity(totalRam) + " RAM");

        //内存类型 ddr4
        Optional<PhysicalMemory> physicalMemoryOptional = globalMemory.getPhysicalMemory().stream().findFirst();
        String ramTypeOrOSBitDepth = physicalMemoryOptional.get().getMemoryType();
        machineDto.setRamTypeOrOSBitDepth(ramTypeOrOSBitDepth);

        //内存使用率
        machineDto.setUsage(getRamUsage(hardware));

        //内存频率
        machineDto.setClockSpeed(getRamFrequency());

        //swap信息
        machineDto.setSwapAmount(getConvertedCapacity(globalMemory.getVirtualMemory().getSwapTotal()) + " Swap");

        return machineDto;
    }

    /**
     * 读取内存信息
     *
     * @return GraphicsDto with filled fields
     */
    private GraphicsDto getGraphics(HardwareAbstractionLayer hardware) {
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        GraphicsDto graphicsDto = new GraphicsDto();
        //显卡信息
        List<GraphicsCard> gpus = hardware.getGraphicsCards();
        GraphicsCard gpu = gpus.get(0);
        //gpu型号
        graphicsDto.setName(gpu.getName());
        //显存大小
        graphicsDto.setMemory(getConvertedCapacity(gpu.getVRam()));

        //显存占用
        graphicsDto.setMemoryUsage(Math.round(gpu.getVRam() * 100.0 / gpu.getVRam()) + "%");

        //gpu占用
        graphicsDto.setUsage(23);

        //gpu频率
        graphicsDto.setClockSpeed("112");
        // 2. 获取详细使用情况（平台特定）
        if (os.getFamily().contains("Windows")) {
            getWindowsGpuDetails();
        } else if (os.getFamily().equals("Linux")) {
            getLinuxGpuDetails();
        } else {
            System.out.println("不支持的操作系统");
        }
        return graphicsDto;
    }

    /**
     * Gets storage information
     *
     * @return StorageDto with filled fields
     */
    private StorageDto getStorage(HardwareAbstractionLayer hardware)
    {
        StorageDto storageDto = new StorageDto();
        List<HWDiskStore> hwDiskStores = hardware.getDiskStores();

    // Retrieve main storage model
        String mainStorage = hwDiskStores.isEmpty() ? "Undefined"
            : hwDiskStores.get(0).getModel().replaceAll("\\(.+?\\)", "").trim();
        storageDto.setMainStorage(mainStorage);

    long total = hwDiskStores.stream().mapToLong(HWDiskStore::getSize).sum();
        storageDto.setTotal(getConvertedCapacity(total) + " Total");

        int diskCount = hwDiskStores.size();
        storageDto.setDiskCount(diskCount + (diskCount > 1 ? " Disks" : " Disk"));


        return storageDto;
    }

    /**
     * Used to deliver dto to corresponding controller
     *
     * @return InfoDto filled with server info
     */
    public InfoDto getInfo() throws ApplicationNotConfiguredException
    {
        if (!Ward.isFirstLaunch())
        {
            InfoDto infoDto = new InfoDto();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();
            //cpu信息
            infoDto.setProcessor(getProcessor(hardware));
            //内存信息
            infoDto.setMachine(getMachine(hardware));
            //gpu信息
            infoDto.setGraphics(getGraphics(hardware));
            //存储信息
            infoDto.setStorage(getStorage(hardware));

            return infoDto;
        }
        else
        {
            throw new ApplicationNotConfiguredException();
        }
    }

    /**
     * cpu占用
     *
     * @return int that display processor usage
     */
    private int getProcessorUsage(HardwareAbstractionLayer hardware) {
        CentralProcessor centralProcessor = hardware.getProcessor();
        long[] prevTicksArray = centralProcessor.getSystemCpuLoadTicks();
        long prevTotalTicks = Arrays.stream(prevTicksArray).sum();
        long prevIdleTicks = prevTicksArray[CentralProcessor.TickType.IDLE.getIndex()];

        Util.sleep(1000);

        long[] currTicksArray = centralProcessor.getSystemCpuLoadTicks();
        long currTotalTicks = Arrays.stream(currTicksArray).sum();
        long currIdleTicks = currTicksArray[CentralProcessor.TickType.IDLE.getIndex()];

        long idleTicksDelta = currIdleTicks - prevIdleTicks;
        long totalTicksDelta = currTotalTicks - prevTotalTicks;

        // Handle possible division by zero
        if (totalTicksDelta == 0) {
            return 0; // or handle in a way suitable for your application
        }

        // Calculate CPU usage percentage
        return (int) ((1 - (double) idleTicksDelta / totalTicksDelta) * 100);
    }

    /**
     * 内存占用
     *
     * @return int that display ram usage
     */
    private int getRamUsage(HardwareAbstractionLayer hardware) {
        GlobalMemory globalMemory = hardware.getMemory();
        long totalMemory = globalMemory.getTotal();
        long availableMemory = globalMemory.getAvailable();

        // Handle possible division by zero
        if (totalMemory == 0) {
            return 0; // or handle in a way suitable for your application
        }

        // Calculate RAM usage percentage
        return (int) (100 - ((double) availableMemory / totalMemory * 100));
    }

    /**
     * 存储空间总占用
     *
     * @return int that display storage usage
     */
    private int getStorageUsage(HardwareAbstractionLayer hardware) {
        FileSystem fileSystem = systemInfo.getOperatingSystem().getFileSystem();

        // Calculate total storage and free storage for all drives
        long totalStorage = 0;
        long freeStorage = 0;
        for (OSFileStore fileStore : fileSystem.getFileStores()) {
            totalStorage += fileStore.getTotalSpace();
            freeStorage += fileStore.getFreeSpace();
        }

        // Handle possible division by zero
        if (totalStorage == 0) {
            return 0; // or handle in a way suitable for your application
        }

        // Calculate total storage usage percentage for all drives
        return (int) Math.round(((double) (totalStorage - freeStorage) / totalStorage) * 100);
    }

    /**
     * 读取内存频率
     *
     * @return 3200 MHz
     */
    private String getRamFrequency(){
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        String ramFrequency = null;
        if (os.getFamily().equals("Linux")) {
            // 方法1: 使用dmidecode
            List<String> dmidecodeOutput = ExecutingCommand.runNative("sudo dmidecode --type memory");
            for (String line : dmidecodeOutput) {
                if (line.contains("Speed:") && !line.contains("Unknown")) {
                    ramFrequency = line.trim() + " MHz";
                }
            }

            // // 方法2: 使用lshw
            // List<String> lshwOutput = ExecutingCommand.runNative("sudo lshw -C memory");
            // for (String line : lshwOutput) {
            //     if (line.contains("clock:") || line.contains("speed:")) {
            //         ramFrequency = line.trim() + "MHz";
            //     }
            // }
        } else if (os.getFamily().contains("Windows")) {
            // Windows系统实现
            List<String> wmicOutput = ExecutingCommand.runNative("wmic memorychip get speed");
            for (String line : wmicOutput) {
                if (!line.trim().equals("Speed") && !line.trim().isEmpty()) {
                    ramFrequency = line.trim() + " MHz";
                }
            }
        } else {
            System.out.println("内存频率读取:不支持的操作系统");
        }
        return ramFrequency;
    }

    private static void getWindowsGpuDetails() {
        // 1. 获取显存大小
        List<String> vramInfo = ExecutingCommand.runNative(
                "wmic path Win32_VideoController where \"AdapterCompatibility like '%Intel%'\" get AdapterRAM /value");
        vramInfo.stream()
                .filter(line -> line.startsWith("AdapterRAM"))
                .findFirst()
                .ifPresent(System.out::println);

        // 2. 获取GPU负载（需要管理员权限）
        List<String> gpuLoad = ExecutingCommand.runNative(
                "wmic path Win32_PerfFormattedData_Counters_GPUEngine where \"Name like '%%eng%%_3D%%'\" get UtilizationPercentage /value");
        System.out.println("GPU负载: " + (gpuLoad.isEmpty() ? "N/A" : gpuLoad.get(0)));

        // 3. 获取显存占用（近似值）
        List<String> memUsage = ExecutingCommand.runNative(
                "wmic path Win32_VideoController where \"AdapterCompatibility like '%Intel%'\" get CurrentHorizontalResolution,CurrentVerticalResolution,VideoMemoryType /value");
        memUsage.forEach(System.out::println);

        // 4. 获取GPU频率（Windows不支持直接查询Intel核显频率）
        System.out.println("注意: Windows下无法直接获取Intel核显频率");
        // 简化示例：实际使用时需要解析WMIC输出
        List<String> result = ExecutingCommand.runNative(
                "wmic path Win32_VideoController get AdapterRAM,CurrentHorizontalResolution,CurrentVerticalResolution /format:list");
        result.forEach(System.out::println);
    }

    private static void getLinuxGpuDetails() {
        // 1. 检查是否安装了intel-gpu-tools
        boolean hasIntelGpuTop = !ExecutingCommand.runNative("which intel_gpu_top").isEmpty();

        // 2. 获取显存信息
        List<String> memInfo = FileUtil.readFile("/proc/meminfo");
        long totalMem = memInfo.stream()
                .filter(line -> line.startsWith("MemTotal"))
                .map(line -> line.replaceAll("\\D+", ""))
                .findFirst()
                .map(Long::parseLong)
                .orElse(0L);

        // Intel核显通常共享系统内存，显存=预分配+动态共享
        List<String> gttInfo = FileUtil.readFile("/sys/kernel/debug/dri/0/i915_gem_gtt");
        System.out.printf("总系统内存: %d MB%n", totalMem / 1024);
        gttInfo.stream()
                .filter(line -> line.contains("Memory"))
                .findFirst()
                .ifPresent(System.out::println);

        // 3. 获取GPU负载和频率
        if (hasIntelGpuTop) {
            List<String> gpuStats = ExecutingCommand.runNative("sudo intel_gpu_top -l 1 -o -");
            gpuStats.stream()
                    .filter(line -> line.contains("GPU busy") || line.contains("MHz"))
                    .forEach(System.out::println);
        } else {
            // 备用方法：从sysfs读取
            List<String> freqInfo = FileUtil.readFile("/sys/class/drm/card0/device/gt_cur_freq_mhz");
            if (!freqInfo.isEmpty()) {
                System.out.println("GPU当前频率: " + freqInfo.get(0) + " MHz");
            }

            List<String> loadInfo = FileUtil.readFile("/sys/class/drm/card0/device/gpu_busy_percent");
            if (!loadInfo.isEmpty()) {
                System.out.println("GPU负载: " + loadInfo.get(0) + "%");
            }
            System.out.println("注意: Linux下无法直接获取Intel核显频率");
        }
    }
}