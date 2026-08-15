package mygame;

// 导入jMonkeyEngine的核心库
import com.jme3.app.SimpleApplication;
import com.jme3.bounding.BoundingBox;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource;
import com.jme3.asset.AssetKey;
import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 主游戏类 - 一个类似Minecraft风格的3D沙盒游戏
 * 使用jMonkeyEngine游戏引擎开发
 */
public class Main extends SimpleApplication implements ActionListener {

    // ============================================================
    // 玩家物理参数 - 控制玩家的移动、跳跃、重力等物理行为
    // ============================================================

    private final Vector3f playerPos = new Vector3f(0, 0f, 0);  // 玩家的三维坐标位置
    private float vy = 0;                                        // 垂直速度（用于跳跃和重力）
    private final float gravity = -25f;                          // 重力加速度（负值表示向下）
    private final float walkSpeed = 4f;                          // 普通行走速度
    private final float sprintSpeed = 7f;                        // 冲刺速度
    private final float jumpForce = 8f;                          // 跳跃力度
    private boolean isGrounded = false;                          // 是否在地面上
    private boolean isSprinting = false;                         // 是否在冲刺

    private boolean isSneaking = false;                          // 是否在潜行
    private final float sneakSpeed = 2f;                         // 潜行速度
    private float currentEyeHeight = EYE_HEIGHT;                 // 当前眼睛高度
    private static final float SNEAK_EYE_HEIGHT = 1.2f;          // 潜行时的眼睛高度

    // 方向控制标志
    private boolean moveForward = false, moveBack = false, moveLeft = false, moveRight = false;

    // 玩家物理常量
    private static final float EYE_HEIGHT = 1.6f;                // 正常眼睛高度
    private static final float PLAYER_RADIUS = 0.3f;            // 玩家碰撞箱半径
    private static final float REACH_DISTANCE = 5f;             // 玩家交互距离

    private static final float PLAYER_HEIGHT = 1.8f;            // 玩家身高
    private static final int COLLISION_LAYERS = 4;              // 碰撞检测层数
    private static final float GROUND_RAY_LENGTH = 1.5f;        // 地面检测射线长度

    // 方块材质
    private Material dirtMat;           // 泥土材质
    private Material stoneMat;          // 石头材质（原diamondMat改为stoneMat）
    private Material currentPlaceMat;   // 当前选中的方块材质
    
    // GUI 材质与对象
    private Material hotbarMat;          // 快捷栏材质
    private Material selectorMat;        // 选择框材质
    private Geometry selectorGeom;       // 选择框几何体
    
    private List<Geometry> slotIcons = new ArrayList<>();  // 快捷栏图标列表

    private int selectedSlot = 0;        // 当前选中的快捷栏槽位（0-8）
    
    // ============================================================
    // ★ 音乐系统
    // ============================================================
    private AudioNode currentMusic;      // 当前播放的音乐节点
    private String currentSongName = ""; // 当前歌曲名称
    private boolean musicLoaded = false; // 音乐是否加载成功
    private boolean musicPlaying = false;// 音乐是否正在播放
    private float musicVolume = 0.3f;    // 音乐音量（0-1）
    private boolean musicError = false;  // 音乐是否有错误
    
    // ★ 歌单列表 - 存储所有音乐文件路径
    private final String[] playlist = {
        "Sounds/music-game-subwoofer_lullaby.ogg",
        "Sounds/music-game-mice_on_venus.ogg",
        "Sounds/music-game-minecraft.ogg"
    };
    
    private int currentSongIndex = 0;    // 当前播放歌曲在歌单中的索引
    private Random random = new Random();// 随机数生成器

    // ★ 快捷栏物品系统
    private Material[] hotbarItems = new Material[9];  // 9个槽位的材质
    private boolean[] hasItem = new boolean[9];        // 每个槽位是否有物品
    private String[] itemNames = new String[9];        // 每个槽位的物品名称

    // ============================================================
    // 程序入口 - main方法，游戏启动点
    // ============================================================
    public static void main(String[] args) {
        Main app = new Main();  // 创建游戏实例
        
        // ★ 设置窗口参数
        AppSettings settings = new AppSettings(true);
        settings.setWidth(1280);          // 窗口宽度
        settings.setHeight(720);          // 窗口高度
        settings.setResizable(true);      // 允许调整窗口大小
        settings.setTitle("Bugjump-Bugcraft-Indev 1.0");  // 窗口标题
        settings.setFullscreen(false);    // 窗口模式（非全屏）
        settings.setVSync(true);          // 开启垂直同步
        
        app.setSettings(settings);
        app.setShowSettings(false);       // 不显示启动设置对话框
        app.start();                      // 启动游戏
    }

    // ============================================================
    // 初始化方法 - 游戏启动时调用一次
    // ============================================================
    @Override
    public void simpleInitApp() {

        // ============================================================
        // 按键绑定 - 将键盘/鼠标事件映射到游戏动作
        // ============================================================

        // 移动控制
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));  // W键前进
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));     // S键后退
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));     // A键左移
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));    // D键右移
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE)); // 空格跳跃
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LCONTROL)); // Ctrl冲刺
        inputManager.addMapping("Sneak", new KeyTrigger(KeyInput.KEY_LSHIFT));    // Shift潜行

        // 方块操作
        inputManager.addMapping("BreakBlock", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));   // 左键破坏方块
        inputManager.addMapping("PlaceBlock", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));  // 右键放置方块

        // 快捷栏选择 - 数字键1-9
        inputManager.addMapping("Slot0", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("Slot1", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("Slot2", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("Slot3", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("Slot4", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("Slot5", new KeyTrigger(KeyInput.KEY_6));
        inputManager.addMapping("Slot6", new KeyTrigger(KeyInput.KEY_7));
        inputManager.addMapping("Slot7", new KeyTrigger(KeyInput.KEY_8));
        inputManager.addMapping("Slot8", new KeyTrigger(KeyInput.KEY_9));

        // ★ 音乐控制按键
        inputManager.addMapping("NextSong", new KeyTrigger(KeyInput.KEY_M));       // M键切换下一首
        inputManager.addMapping("ToggleMusic", new KeyTrigger(KeyInput.KEY_N));    // N键播放/暂停
        inputManager.addMapping("VolumeUp", new KeyTrigger(KeyInput.KEY_RBRACKET));   // ]键增大音量
        inputManager.addMapping("VolumeDown", new KeyTrigger(KeyInput.KEY_LBRACKET)); // [键减小音量

        // 注册动作监听器
        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Jump", "Sprint", "Sneak",
                "BreakBlock", "PlaceBlock", "Slot0", "Slot1", "Slot2", "Slot3", "Slot4", 
                "Slot5", "Slot6", "Slot7", "Slot8",
                "NextSong", "ToggleMusic", "VolumeUp", "VolumeDown");

        // 设置摄像机控制
        inputManager.setCursorVisible(false);  // 隐藏鼠标光标
        flyCam.setDragToRotate(false);         // 不需要拖动旋转
        flyCam.setMoveSpeed(0);                // 禁用默认移动

        // 删除默认的飞行控制映射（避免冲突）
        inputManager.deleteMapping("FLYCAM_Left");
        inputManager.deleteMapping("FLYCAM_Right");
        inputManager.deleteMapping("FLYCAM_Up");
        inputManager.deleteMapping("FLYCAM_Down");
        inputManager.deleteMapping("FLYCAM_Forward");
        inputManager.deleteMapping("FLYCAM_Backward");
        inputManager.deleteMapping("FLYCAM_Rise");
        inputManager.deleteMapping("FLYCAM_Lower");

        // ============================================================
        // 方块与GUI材质加载 - 加载游戏需要的纹理和材质
        // ============================================================

        // 加载泥土材质
        dirtMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        dirtMat.setTexture("ColorMap", assetManager.loadTexture("Textures/dirt.png"));

        // 加载石头材质（原diamondMat改为stoneMat）
        stoneMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        stoneMat.setTexture("ColorMap", assetManager.loadTexture("Textures/stone.png"));
        
        // 加载快捷栏材质
        hotbarMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture hotbarTex = assetManager.loadTexture("Textures/gui.png");
        hotbarTex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);  // 设置纹理过滤
        hotbarTex.setMagFilter(Texture.MagFilter.Nearest);
        hotbarMat.setTexture("ColorMap", hotbarTex);

        // 加载选择框材质
        selectorMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        Texture selTex = assetManager.loadTexture("Textures/gui1.png");
        selTex.setMinFilter(Texture.MinFilter.NearestNoMipMaps);
        selTex.setMagFilter(Texture.MagFilter.Nearest);
        selectorMat.setTexture("ColorMap", selTex);

        // ★ 初始化快捷栏物品
        hotbarItems[0] = dirtMat;     // 第1格：泥土
        hasItem[0] = true;
        itemNames[0] = "Dirt";
        
        hotbarItems[1] = stoneMat;    // 第2格：石头（原diamondMat改为stoneMat）
        hasItem[1] = true;
        itemNames[1] = "Stone";
        
        // 第3-9格为空
        for (int i = 2; i < 9; i++) {
            hotbarItems[i] = null;
            hasItem[i] = false;
            itemNames[i] = "Empty";
        }
        
        currentPlaceMat = dirtMat;  // 默认选中泥土

        // ============================================================
        // 地形生成 - 生成一个平坦的地面
        // ============================================================

        int mapSize = 50;  // 地图大小
        Box blockBox = new Box(0.5f, 0.5f, 0.5f);  // 方块的几何体（边长1）

        // 生成泥土层（地表）
        for (int x = 0; x < mapSize; x++) {
            for (int z = 0; z < mapSize; z++) {
                Geometry geom = new Geometry("DirtBlock", blockBox);
                geom.setLocalTranslation(x, -0.5f, z);  // 设置位置
                geom.setMaterial(dirtMat);               // 设置材质
                geom.setModelBound(new BoundingBox());   // 设置碰撞箱
                geom.updateModelBound();
                rootNode.attachChild(geom);              // 添加到场景中
            }
        }

        // 生成石头层（地下层）
        for (int x = 0; x < mapSize; x++) {
            for (int z = 0; z < mapSize; z++) {
                Geometry geom = new Geometry("StoneBlock", blockBox);
                geom.setLocalTranslation(x, -1.5f, z);
                geom.setMaterial(stoneMat);              // 使用stoneMat
                geom.setModelBound(new BoundingBox());
                geom.updateModelBound();
                rootNode.attachChild(geom);
            }
        }

        // 设置摄像机初始位置
        cam.setLocation(playerPos.add(0, EYE_HEIGHT, 0));
        cam.lookAt(new Vector3f(0, 0, 0), Vector3f.UNIT_Y);

        // 绘制GUI界面
        drawGUI();
        
        // ============================================================
        // ★ 初始化音乐系统
        // ============================================================
        initializeMusic();
    }

    // ============================================================
    // ★ 音乐系统方法
    // ============================================================
    
    /**
     * 初始化音乐系统 - 随机选择一首歌开始播放
     */
    private void initializeMusic() {
        System.out.println("========================================");
        System.out.println("[Music] Initializing...");
        
        if (playlist.length == 0) {
            System.err.println("[Music] Playlist is empty!");
            return;
        }
        
        System.out.println("[Music] Playlist:");
        for (int i = 0; i < playlist.length; i++) {
            System.out.println("  " + (i+1) + ". " + playlist[i]);
        }
        
        currentSongIndex = random.nextInt(playlist.length);  // 随机选择
        String songPath = playlist[currentSongIndex];
        System.out.println("[Music] Selected: " + songPath);
        
        loadSong(songPath);  // 加载并播放
        
        System.out.println("========================================");
        System.out.println("[Music] Controls:");
        System.out.println("  M - Next song");
        System.out.println("  N - Play/Pause");
        System.out.println("  [ - Volume down");
        System.out.println("  ] - Volume up");
        System.out.println("========================================");
    }
    
    /**
     * 加载歌曲 - 从文件中加载音乐并播放
     * @param songPath 音乐文件路径
     */
    private void loadSong(String songPath) {
        try {
            if (currentMusic != null) {
                currentMusic.stop();                      // 停止当前播放
                currentMusic.removeFromParent();          // 从场景中移除
                currentMusic = null;
            }
            
            System.out.println("[Music] Loading: " + songPath);
            
            // 检查文件是否存在
            AssetKey<?> assetKey = new AssetKey<>(songPath);
            if (assetManager.locateAsset(assetKey) == null) {
                System.err.println("[Music] File not found: " + songPath);
                musicError = true;
                return;
            }
            
            // 创建音频节点
            currentMusic = new AudioNode(assetManager, songPath);
            currentMusic.setPositional(false);   // 不启用3D音效
            currentMusic.setLooping(true);       // 循环播放
            currentMusic.setVolume(musicVolume); // 设置音量
            
            rootNode.attachChild(currentMusic);
            currentMusic.play();                 // 开始播放
            musicPlaying = true;
            musicLoaded = true;
            musicError = false;
            
            String[] parts = songPath.split("/");
            currentSongName = parts[parts.length - 1];
            
            System.out.println("[Music] Loaded: " + currentSongName);
            System.out.println("[Music] Volume: " + (int)(musicVolume * 100) + "%");
            
        } catch (Exception e) {
            System.err.println("[Music] Load failed: " + e.getMessage());
            musicLoaded = false;
            musicPlaying = false;
            musicError = true;
        }
    }
    
    /**
     * 切换音乐播放/暂停
     */
    private void toggleMusic() {
        if (currentMusic == null) {
            System.out.println("[Music] No music");
            return;
        }
        
        if (musicPlaying) {
            currentMusic.pause();           // 暂停
            musicPlaying = false;
            System.out.println("[Music] Paused");
        } else {
            currentMusic.play();            // 继续播放
            musicPlaying = true;
            System.out.println("[Music] Resumed");
        }
    }
    
    /**
     * 切换到下一首歌
     */
    private void nextSong() {
        if (playlist.length == 0) return;
        
        currentSongIndex = (currentSongIndex + 1) % playlist.length;  // 循环
        String nextSongPath = playlist[currentSongIndex];
        System.out.println("[Music] Next: " + nextSongPath);
        loadSong(nextSongPath);
    }
    
    /**
     * 增加音量
     */
    private void volumeUp() {
        musicVolume = Math.min(1.0f, musicVolume + 0.1f);  // 最大1.0
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }
        System.out.println("[Music] Volume: " + (int)(musicVolume * 100) + "%");
    }
    
    /**
     * 减小音量
     */
    private void volumeDown() {
        musicVolume = Math.max(0.0f, musicVolume - 0.1f);  // 最小0.0
        if (currentMusic != null) {
            currentMusic.setVolume(musicVolume);
        }
        System.out.println("[Music] Volume: " + (int)(musicVolume * 100) + "%");
    }
    
    /**
     * 更新音乐状态 - 每帧调用
     */
    private void updateMusic() {
        // 音乐循环播放，由 AudioNode 自己管理
        // 不需要额外检查
    }

    // ============================================================
    // 按键响应逻辑 - 处理玩家输入
    // ============================================================
    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            // 移动控制
            case "Forward": moveForward = isPressed; break;
            case "Back": moveBack = isPressed; break;
            case "Left": moveLeft = isPressed; break;
            case "Right": moveRight = isPressed; break;
            case "Sprint": isSprinting = isPressed; break;
            case "Sneak": isSneaking = isPressed; break;
            // 跳跃
            case "Jump":
                if (isPressed && isGrounded) { 
                    vy = jumpForce; 
                    isGrounded = false; 
                }
                break;
            // 方块操作
            case "BreakBlock": if (isPressed) breakBlock(); break;
            case "PlaceBlock": if (isPressed) placeBlock(); break;
            // 快捷栏选择
            case "Slot0": if (isPressed) setSelectedSlot(0); break;
            case "Slot1": if (isPressed) setSelectedSlot(1); break;
            case "Slot2": if (isPressed) setSelectedSlot(2); break;
            case "Slot3": if (isPressed) setSelectedSlot(3); break;
            case "Slot4": if (isPressed) setSelectedSlot(4); break;
            case "Slot5": if (isPressed) setSelectedSlot(5); break;
            case "Slot6": if (isPressed) setSelectedSlot(6); break;
            case "Slot7": if (isPressed) setSelectedSlot(7); break;
            case "Slot8": if (isPressed) setSelectedSlot(8); break;
            // 音乐控制
            case "NextSong": if (isPressed) nextSong(); break;
            case "ToggleMusic": if (isPressed) toggleMusic(); break;
            case "VolumeUp": if (isPressed) volumeUp(); break;
            case "VolumeDown": if (isPressed) volumeDown(); break;
        }
    }
    
    // ============================================================
    // ★ 快捷栏选择方法
    // ============================================================
    /**
     * 设置选中的快捷栏槽位
     * @param slot 槽位索引（0-8）
     */
    private void setSelectedSlot(int slot) {
        if (this.selectedSlot != slot) {
            this.selectedSlot = slot;
            
            // ★ 检查该槽位是否有物品
            if (hasItem[slot] && hotbarItems[slot] != null) {
                currentPlaceMat = hotbarItems[slot];
                System.out.println("[Slot] " + (slot + 1) + ": " + itemNames[slot] + " ✓");
            } else {
                currentPlaceMat = null;  // 没有物品，不能放置
                System.out.println("[Slot] " + (slot + 1) + ": Empty ✗");
            }
            
            updateSelectorPosition();  // 更新选择框位置
        }
    }

    // ============================================================
    // 射线检测功能 - 用于检测玩家瞄准的方块
    // ============================================================

    /**
     * 获取玩家瞄准的目标方块
     * @return 碰撞结果，如果没有则返回null
     */
    private CollisionResult getTargetBlock() {
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());  // 从摄像机发射射线
        ray.setLimit(REACH_DISTANCE);                              // 设置射程
        CollisionResults results = new CollisionResults();
        rootNode.collideWith(ray, results);                        // 检测与场景的碰撞
        return results.size() > 0 ? results.getClosestCollision() : null;
    }

    /**
     * 破坏方块 - 移除玩家瞄准的方块
     */
    private void breakBlock() {
        CollisionResult hit = getTargetBlock();
        if (hit != null && hit.getGeometry() != null && hit.getGeometry().getParent() == rootNode) {
            hit.getGeometry().removeFromParent();  // 从场景中移除
        }
    }

    // ============================================================
    // ★ 放置方块方法
    // ============================================================
    /**
     * 放置方块 - 在玩家瞄准的位置放置当前选中的方块
     */
    private void placeBlock() {
        // ★ 检查当前选中的是否有物品
        if (currentPlaceMat == null) {
            System.out.println("[Place] No item selected!");
            return;
        }
        
        CollisionResult hit = getTargetBlock();
        if (hit == null || hit.getGeometry() == null) return;

        // 获取被瞄准方块的坐标
        Vector3f hitBlockPos = hit.getGeometry().getLocalTranslation();
        // 获取碰撞法线方向，决定新方块放置在哪一面
        Vector3f normal = hit.getContactNormal().clone();
        int nx = Math.round(normal.x);
        int ny = Math.round(normal.y);
        int nz = Math.round(normal.z);
        normal.set(nx, ny, nz);
        Vector3f newPos = hitBlockPos.add(normal);  // 新方块位置

        // 检查新方块是否会与玩家重叠
        float dx = newPos.x - playerPos.x;
        float dy = newPos.y - playerPos.y;
        float dz = newPos.z - playerPos.z;
        if (Math.abs(dx) < (0.5f + PLAYER_RADIUS) &&
            dy > -0.5f && dy < PLAYER_HEIGHT &&
            Math.abs(dz) < (0.5f + PLAYER_RADIUS)) {
            return;  // 会重叠，不放置
        }

        // 创建并放置方块
        Box box = new Box(0.5f, 0.5f, 0.5f);
        Geometry geom = new Geometry("PlacedBlock", box);
        geom.setLocalTranslation(newPos);
        geom.setMaterial(currentPlaceMat);
        geom.setModelBound(new BoundingBox());
        geom.updateModelBound();
        rootNode.attachChild(geom);
        
        System.out.println("[Place] Placed: " + itemNames[selectedSlot]);
    }

    // ============================================================
    // 游戏循环 - 每帧更新
    // ============================================================
    @Override
    public void simpleUpdate(float tpf) {
        // 计算移动方向
        Vector3f forward = cam.getDirection().clone();
        forward.y = 0;
        forward.normalizeLocal();
        Vector3f left = cam.getLeft().clone();
        left.y = 0;
        left.normalizeLocal();

        // 计算速度
        float currentSpeed = isSneaking ? sneakSpeed : (isSprinting ? sprintSpeed : walkSpeed);
        Vector3f move = new Vector3f();
        if (moveForward) move.addLocal(forward.mult(currentSpeed * tpf));
        if (moveBack)   move.addLocal(forward.mult(-currentSpeed * tpf));
        if (moveLeft)   move.addLocal(left.mult(currentSpeed * tpf));
        if (moveRight)  move.addLocal(left.mult(-currentSpeed * tpf));

        // 处理移动（带碰撞检测）
        if (isSneaking) {
            // 潜行时有限制移动
            float testX = playerPos.x + move.x;
            if (hasSupportAt(testX, playerPos.z) && !checkHorizontalCollision(new Vector3f(testX, playerPos.y, playerPos.z))) playerPos.x = testX;
            float testZ = playerPos.z + move.z;
            if (hasSupportAt(playerPos.x, testZ) && !checkHorizontalCollision(new Vector3f(playerPos.x, playerPos.y, testZ))) playerPos.z = testZ;
        } else {
            // 正常移动
            float testX = playerPos.x + move.x;
            if (!checkHorizontalCollision(new Vector3f(testX, playerPos.y, playerPos.z))) playerPos.x = testX;
            float testZ = playerPos.z + move.z;
            if (!checkHorizontalCollision(new Vector3f(playerPos.x, playerPos.y, testZ))) playerPos.z = testZ;
        }

        // 处理重力和跳跃
        vy += gravity * tpf;
        float targetY = playerPos.y + vy * tpf;
        isGrounded = false;

        // 检测地面
        Ray downRay = new Ray(new Vector3f(playerPos.x, playerPos.y, playerPos.z), Vector3f.UNIT_Y.negate());
        downRay.setLimit(GROUND_RAY_LENGTH);
        CollisionResults results = new CollisionResults();
        rootNode.collideWith(downRay, results);

        if (results.size() > 0) {
            CollisionResult closest = results.getClosestCollision();
            float groundY = closest.getContactPoint().y;
            if (targetY <= groundY && vy <= 0) {
                targetY = groundY;
                vy = 0;
                isGrounded = true;
            }
        }

        // 掉到世界下方时重置位置
        if (playerPos.y < -10) {
            playerPos.set(5, 5, 5);
            vy = 0;
            currentEyeHeight = EYE_HEIGHT;
        }
        playerPos.y = targetY;

        // 更新摄像机位置（平滑过渡潜行高度）
        float targetEyeHeight = isSneaking ? SNEAK_EYE_HEIGHT : EYE_HEIGHT;
        currentEyeHeight += (targetEyeHeight - currentEyeHeight) * Math.min(tpf * 10f, 1f);
        cam.setLocation(new Vector3f(playerPos.x, playerPos.y + currentEyeHeight, playerPos.z));
        
        // ★ 更新音乐
        updateMusic();
    }

    // ============================================================
    // 碰撞检测辅助方法
    // ============================================================

    /**
     * 检测水平方向的碰撞
     * @param targetPos 目标位置
     * @return 如果有碰撞返回true
     */
    private boolean checkHorizontalCollision(Vector3f targetPos) {
        Vector3f[] directions = { Vector3f.UNIT_X, Vector3f.UNIT_X.negate(), Vector3f.UNIT_Z, Vector3f.UNIT_Z.negate() };
        for (int i = 0; i < COLLISION_LAYERS; i++) {
            float layerY = targetPos.y + (PLAYER_HEIGHT / COLLISION_LAYERS) * i + 0.1f;
            Vector3f origin = new Vector3f(targetPos.x, layerY, targetPos.z);
            for (Vector3f dir : directions) {
                Ray ray = new Ray(origin, dir);
                ray.setLimit(PLAYER_RADIUS + 0.05f);
                CollisionResults results = new CollisionResults();
                rootNode.collideWith(ray, results);
                if (results.size() > 0) return true;
            }
        }
        return false;
    }

    /**
     * 检测指定位置是否有支撑（用于潜行）
     * @param x X坐标
     * @param z Z坐标
     * @return 如果有支撑返回true
     */
    private boolean hasSupportAt(float x, float z) {
        Ray supportRay = new Ray(new Vector3f(x, playerPos.y, z), Vector3f.UNIT_Y.negate());
        supportRay.setLimit(GROUND_RAY_LENGTH);
        CollisionResults supportResults = new CollisionResults();
        rootNode.collideWith(supportRay, supportResults);
        return supportResults.size() > 0;
    }

    // ============================================================
    // GUI界面 - 绘制快捷栏和准星
    // ============================================================

    /**
     * 绘制GUI界面
     */
    private void drawGUI() {
        float barWidth = 580f;    // 快捷栏宽度
        float barHeight = 60f;    // 快捷栏高度
        float xPos = settings.getWidth() / 2f - (barWidth / 2f);  // 水平居中
        float yPos = 20;          // 底部位置

        // 绘制快捷栏背景
        Quad barQuad = new Quad(barWidth, barHeight);
        Geometry barGeom = new Geometry("Hotbar", barQuad);
        barGeom.setMaterial(hotbarMat);
        barGeom.setLocalTranslation(xPos, yPos, 0); 
        guiNode.attachChild(barGeom);

        // 绘制快捷栏图标
        float iconSize = 30f;
        float slotWidth = barWidth / 9.0f;
        float centerYOffset = (barHeight - iconSize) / 2f;

        // 泥土图标
        Geometry dirtIcon = createIcon(dirtMat, iconSize);
        float iconX0 = xPos + (0 * slotWidth) + (slotWidth - iconSize) / 2f;
        dirtIcon.setLocalTranslation(iconX0, yPos + centerYOffset, 2);
        guiNode.attachChild(dirtIcon);
        slotIcons.add(dirtIcon);

        // 石头图标（原diamondMat改为stoneMat）
        Geometry stoneIcon = createIcon(stoneMat, iconSize);
        float iconX1 = xPos + (1 * slotWidth) + (slotWidth - iconSize) / 2f;
        stoneIcon.setLocalTranslation(iconX1, yPos + centerYOffset, 2);
        guiNode.attachChild(stoneIcon);
        slotIcons.add(stoneIcon);

        // 绘制选择框
        Quad selQuad = new Quad(slotWidth, barHeight);
        selectorGeom = new Geometry("Selector", selQuad);
        selectorGeom.setMaterial(selectorMat);
        guiNode.attachChild(selectorGeom);
        
        updateSelectorPosition();  // 更新选择框位置
        drawCrosshair(settings.getHeight() / 2f + 20);  // 绘制准星
    }

    /**
     * 创建图标
     * @param mat 材质
     * @param size 大小
     * @return 几何体
     */
    private Geometry createIcon(Material mat, float size) {
        Quad q = new Quad(size, size);
        Geometry g = new Geometry("Icon", q);
        g.setMaterial(mat);
        return g;
    }

    /**
     * 更新选择框位置
     */
    private void updateSelectorPosition() {
        if (selectorGeom == null) return;
        float barWidth = 580f;
        float xPos = settings.getWidth() / 2f - (barWidth / 2f);
        float yPos = 20; 
        float slotWidth = barWidth / 9.0f;
        float selX = xPos + (selectedSlot * slotWidth);
        selectorGeom.setLocalTranslation(selX, yPos, 1);
    }

    /**
     * 绘制准星
     * @param yPos Y坐标
     */
    private void drawCrosshair(float yPos) {
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);
        float screenCenterX = settings.getWidth() / 2f;

        // 垂直线
        Quad verticalQuad = new Quad(2, 20);
        Geometry vLine = new Geometry("VLine", verticalQuad);
        vLine.setMaterial(mat);
        vLine.setLocalTranslation(screenCenterX - 1, yPos - 10, 0);
        guiNode.attachChild(vLine);

        // 水平线
        Quad horizontalQuad = new Quad(20, 2);
        Geometry hLine = new Geometry("HLine", horizontalQuad);
        hLine.setMaterial(mat);
        hLine.setLocalTranslation(screenCenterX - 10, yPos - 1, 0);
        guiNode.attachChild(hLine);
    }
}