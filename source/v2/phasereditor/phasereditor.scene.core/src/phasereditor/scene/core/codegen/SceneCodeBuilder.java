// The MIT License (MIT)
//
// Copyright (c) 2015, 2018 Arian Fornaris
//
// Permission is hereby granted, free of charge, to any person obtaining a
// copy of this software and associated documentation files (the
// "Software"), to deal in the Software without restriction, including
// without limitation the rights to use, copy, modify, merge, publish,
// distribute, sublicense, and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so, subject to the
// following conditions: The above copyright notice and this permission
// notice shall be included in all copies or substantial portions of the
// Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
// OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
// MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
// NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
// USE OR OTHER DEALINGS IN THE SOFTWARE.
package phasereditor.scene.core.codegen;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IFile;

import phasereditor.assetpack.core.IAssetFrameModel;
import phasereditor.assetpack.core.ImageAssetModel;
import phasereditor.assetpack.core.SpritesheetAssetModel;
import phasereditor.project.core.ProjectCore;
import phasereditor.scene.core.EditorComponent;
import phasereditor.scene.core.ObjectModel;
import phasereditor.scene.core.OriginComponent;
import phasereditor.scene.core.ParentComponent;
import phasereditor.scene.core.SpriteModel;
import phasereditor.scene.core.TextureComponent;
import phasereditor.scene.core.TileSpriteComponent;
import phasereditor.scene.core.TileSpriteModel;
import phasereditor.scene.core.TransformComponent;
import phasereditor.scene.core.WorldModel;
import phasereditor.scene.core.codedom.ClassDeclDom;
import phasereditor.scene.core.codedom.MethodCallDom;
import phasereditor.scene.core.codedom.MethodDeclDom;
import phasereditor.scene.core.codedom.RawCode;
import phasereditor.scene.core.codedom.UnitDom;

/**
 * @author arian
 *
 */
public class SceneCodeBuilder {

	private IFile _file;

	public SceneCodeBuilder(IFile file) {
		_file = file;
	}

	public UnitDom build(WorldModel model) {

		var unit = new UnitDom();

		var clsName = _file.getFullPath().removeFileExtension().lastSegment();

		var clsDom = new ClassDeclDom(clsName);
		clsDom.setSuperClass("Phaser.Scene");

		var preloadDom = buildPreloadMethod(model);

		var createDom = buildCreateMethod(model);

		clsDom.getMembers().add(preloadDom);
		clsDom.getMembers().add(createDom);

		unit.getElements().add(clsDom);

		return unit;
	}

	private MethodDeclDom buildCreateMethod(WorldModel model) {
		var methodDecl = new MethodDeclDom("create");

		for (var child : ParentComponent.get_children(model)) {
			MethodCallDom methodCall = null;
			if (child instanceof TileSpriteModel) {
				methodCall = buildCreateTileSprite(methodDecl, (TileSpriteModel) child);
			} else if (child instanceof SpriteModel) {
				methodCall = buildCreateSprite(methodDecl, (SpriteModel) child);
			}

			var assignToVar = false;

			if (child instanceof OriginComponent) {
				assignToVar = buildOrigin(methodDecl, child) || assignToVar;
			}

			if (assignToVar && methodCall != null) {
				methodCall.setReturnToVar(EditorComponent.get_editorName(child));
			}
		}

		return methodDecl;
	}

	@SuppressWarnings("static-method")
	private boolean buildOrigin(MethodDeclDom methodDecl, ObjectModel model) {

		var x = OriginComponent.get_originX(model);
		var y = OriginComponent.get_originY(model);

		if (x == OriginComponent.originX_default && y == OriginComponent.originY_default) {
			return false;
		}

		var name = EditorComponent.get_editorName(model);
		var call = new MethodCallDom("setOrigin", name);

		call.arg(OriginComponent.get_originX(model));
		call.arg(OriginComponent.get_originY(model));

		methodDecl.getInstructions().add(call);

		return true;
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateSprite(MethodDeclDom methodDecl, SpriteModel model) {
		var call = new MethodCallDom("sprite", "this.add");

		call.arg((int) TransformComponent.get_x(model));
		call.arg((int) TransformComponent.get_y(model));

		var frame = TextureComponent.get_frame(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	@SuppressWarnings("static-method")
	private MethodCallDom buildCreateTileSprite(MethodDeclDom methodDecl, TileSpriteModel model) {

		var call = new MethodCallDom("tileSprite", "this.add");

		call.arg((int) TransformComponent.get_x(model));
		call.arg((int) TransformComponent.get_y(model));

		call.arg((int) TileSpriteComponent.get_width(model));
		call.arg((int) TileSpriteComponent.get_height(model));

		var frame = TextureComponent.get_frame(model);

		buildTextureArguments(call, frame);

		methodDecl.getInstructions().add(call);

		return call;
	}

	private static void buildTextureArguments(MethodCallDom call, IAssetFrameModel frame) {
		var asset = frame.getAsset();

		if (asset instanceof ImageAssetModel) {
			call.argLiteral(asset.getKey());
		} else {
			call.argLiteral(asset.getKey());
			if (frame instanceof SpritesheetAssetModel.FrameModel) {
				call.arg(((SpritesheetAssetModel.FrameModel) frame).getIndex());
			} else {
				call.argLiteral(frame.getKey());
			}
		}
	}

	@SuppressWarnings("static-method")
	private MethodDeclDom buildPreloadMethod(WorldModel model) {

		var preloadDom = new MethodDeclDom("preload");

		Map<String, String[]> packSectionList = new HashMap<>();

		model.visit(objModel -> {
			if (objModel instanceof TextureComponent) {
				var frame = TextureComponent.get_frame(objModel);
				if (frame != null) {

					var pack = ProjectCore.getAssetUrl(frame.getAsset().getPack().getFile());
					var section = frame.getAsset().getSection().getKey();

					packSectionList.put(section + "-" + pack, new String[] { section, pack });
				}
			}
		});

		for (var pair : packSectionList.values()) {
			var line = new RawCode("this.load.pack('" + pair[0] + "', '" + pair[1] + "');");
			preloadDom.getInstructions().add(line);
		}

		return preloadDom;
	}

}