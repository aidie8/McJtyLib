package mcjty.lib.container;

import mcjty.lib.McJtyRegister;
import mcjty.lib.base.ModBase;
import mcjty.lib.compat.theoneprobe.TOPInfoProvider;
import mcjty.lib.compat.waila.WailaInfoProvider;
import mcjty.lib.varia.OrientationTools;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.ProbeMode;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collection;
import java.util.List;

@Optional.InterfaceList({
        @Optional.Interface(iface = "mcjty.lib.compat.waila.WailaInfoProvider", modid = "waila"),
        @Optional.Interface(iface = "mcjty.lib.compat.theoneprobe.TOPInfoProvider", modid = "theoneprobe")
})
public class BaseBlock extends Block implements WailaInfoProvider, TOPInfoProvider {

    protected ModBase modBase;
    private boolean creative;

    public static final PropertyDirection FACING_HORIZ = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);
    public static final PropertyDirection FACING = PropertyDirection.create("facing");

    public BaseBlock(ModBase mod,
                        Material material,
                        String name,
                        Class<? extends ItemBlock> itemBlockClass) {
        super(material);
        this.modBase = mod;
        this.creative = false;
        setHardness(2.0f);
        setSoundType(SoundType.METAL);
        setHarvestLevel("pickaxe", 0);
        setUnlocalizedName(mod.getModId() + "." + name);
        setRegistryName(name);
        McJtyRegister.registerLater(this, mod, itemBlockClass);
    }

    public BaseBlock setCreative(boolean creative) {
        this.creative = creative;
        return this;
    }

    public boolean isCreative() {
        return creative;
    }

    public static Collection<IProperty<?>> getPropertyKeys(IBlockState state) {
        return state.getPropertyKeys();
    }

    public static boolean activateBlock(Block block, World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        return block.onBlockActivated(world, pos, state, player, hand, facing, hitX, hitY, hitZ);
    }

    public enum RotationType {
        HORIZROTATION,
        ROTATION,
        NONE
    }

    public RotationType getRotationType() {
        return RotationType.ROTATION;
    }

    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
        switch (getRotationType()) {
            case HORIZROTATION:
                return state.withProperty(FACING_HORIZ, placer.getHorizontalFacing().getOpposite());
            case ROTATION:
                return state.withProperty(FACING, OrientationTools.getFacingFromEntity(pos, placer));
            default:
                return state;
        }
    }

    protected EnumFacing getOrientation(BlockPos pos, EntityLivingBase entityLivingBase) {
        switch (getRotationType()) {
            case HORIZROTATION:
                return OrientationTools.determineOrientationHoriz(entityLivingBase);
            case ROTATION:
                return OrientationTools.determineOrientation(pos, entityLivingBase);
            default:
                return null;
        }
    }

    public EnumFacing getFrontDirection(IBlockState state) {
        switch (getRotationType()) {
            case HORIZROTATION:
                return state.getValue(FACING_HORIZ);
            case ROTATION:
                return state.getValue(FACING);
            default:
                return EnumFacing.NORTH;
        }
    }

    public EnumFacing getRightDirection(IBlockState state) {
        return getFrontDirection(state).rotateYCCW();
    }

    public EnumFacing getLeftDirection(IBlockState state) {
        return getFrontDirection(state).rotateY();
    }

    public static EnumFacing getFrontDirection(RotationType metaUsage, IBlockState state) {
        switch (metaUsage) {
            case HORIZROTATION:
                return OrientationTools.getOrientationHoriz(state);
            case ROTATION:
                return OrientationTools.getOrientation(state);
            default:
                return EnumFacing.SOUTH;
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        switch (getRotationType()) {
            case HORIZROTATION:
                return getDefaultState().withProperty(FACING_HORIZ, EnumFacing.VALUES[meta + 2]);
            case ROTATION:
                return getDefaultState().withProperty(FACING, EnumFacing.VALUES[meta & 7]);
            default:
                return getDefaultState();
        }
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        switch (getRotationType()) {
            case HORIZROTATION:
                return state.getValue(FACING_HORIZ).getIndex()-2;
            case ROTATION:
                return state.getValue(FACING).getIndex();
            default:
                return 0;
        }
    }

    protected IProperty<?>[] getProperties() {
        return getProperties(getRotationType());
    }

    public static IProperty<?>[] getProperties(RotationType rotationType) {
        switch (rotationType) {
            case HORIZROTATION:
                return new IProperty[] { FACING_HORIZ };
            case ROTATION:
                return new IProperty[] { FACING };
            default:
                return new IProperty[0];
        }
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, getProperties());
    }

    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
    }

    @Override
    @SideOnly(Side.CLIENT)
    @Optional.Method(modid = "waila")
    public List<String> getWailaBody(ItemStack itemStack, List<String> currenttip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return currenttip;
    }
}
