var Opcodes = Java.type('org.objectweb.asm.Opcodes')
var InsnList = Java.type('org.objectweb.asm.tree.InsnList')
var TypeInsnNode = Java.type('org.objectweb.asm.tree.TypeInsnNode')
var MethodInsnNode = Java.type('org.objectweb.asm.tree.MethodInsnNode')
var InsnNode = Java.type('org.objectweb.asm.tree.InsnNode')
//This is a debug utility. It can't be done via mixins because mixins doesn't do well with synthetic methods.
//This isn't loaded by default. To load it you'd need to add "logger": "coremods/logger.js" to coremods.json
function initializeCoreMod() {
	return {
		'debug_logger': {
			'target': {
				'type': 'METHOD',
				'class': 'net.minecraft.world.entity.EntityType',
				'methodName': 'm_185999_',
				'methodDesc': '(Lnet/minecraft/nbt/CompoundTag;)V'
			},
			'transformer': function(node) {
				//new RuntimeException().printStackTrace();
				var ls = new InsnList();
				ls.add(new TypeInsnNode(Opcodes.NEW, "java/lang/RuntimeException"))
				ls.add(new InsnNode(Opcodes.DUP))
				ls.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V", false));
				ls.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/RuntimeException", "printStackTrace", "()V", false));
				node.instructions.insert(ls);
				return node;
			}
		}
	}
}