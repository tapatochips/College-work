///////////////////////////////////////////////////////////////////////////////
// shadermanager.cpp
// ============
// manage the loading and rendering of 3D scenes
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#include "SceneManager.h"

#ifndef STB_IMAGE_IMPLEMENTATION
#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"
#endif

#include <glm/gtx/transform.hpp>

// declaration of global variables
namespace
{
	const char* g_ModelName = "model";
	const char* g_ColorValueName = "objectColor";
	const char* g_TextureValueName = "objectTexture";
	const char* g_UseTextureName = "bUseTexture";
	const char* g_UseLightingName = "bUseLighting";
}

/***********************************************************
 *  SceneManager()
 *
 *  The constructor for the class
 ***********************************************************/
SceneManager::SceneManager(ShaderManager *pShaderManager)
{
	m_pShaderManager = pShaderManager;
	m_basicMeshes = new ShapeMeshes();

	m_numLights = 0;
	//init lights
	for (int i = 0; i < MAX_LIGHTS; i++)
	{
		m_lights[i].enabled = false;
	}
}

/***********************************************************
 *  ~SceneManager()
 *
 *  The destructor for the class
 ***********************************************************/
SceneManager::~SceneManager()
{
	m_pShaderManager = NULL;
	delete m_basicMeshes;
	m_basicMeshes = NULL;
}

/***********************************************************
 *  CreateGLTexture()
 *
 *  This method is used for loading textures from image files,
 *  configuring the texture mapping parameters in OpenGL,
 *  generating the mipmaps, and loading the read texture into
 *  the next available texture slot in memory.
 ***********************************************************/
bool SceneManager::CreateGLTexture(const char* filename, std::string tag)
{
	int width = 0;
	int height = 0;
	int colorChannels = 0;
	GLuint textureID = 0;

	// indicate to always flip images vertically when loaded
	stbi_set_flip_vertically_on_load(true);

	// try to parse the image data from the specified image file
	unsigned char* image = stbi_load(
		filename,
		&width,
		&height,
		&colorChannels,
		0);

	// if the image was successfully read from the image file
	if (image)
	{
		std::cout << "Successfully loaded image:" << filename << ", width:" << width << ", height:" << height << ", channels:" << colorChannels << std::endl;

		glGenTextures(1, &textureID);
		glBindTexture(GL_TEXTURE_2D, textureID);

		// set the texture wrapping parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		// set texture filtering parameters
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		// if the loaded image is in RGB format
		if (colorChannels == 3)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, image);
		// if the loaded image is in RGBA format - it supports transparency
		else if (colorChannels == 4)
			glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, image);
		else
		{
			std::cout << "Not implemented to handle image with " << colorChannels << " channels" << std::endl;
			return false;
		}

		// generate the texture mipmaps for mapping textures to lower resolutions
		glGenerateMipmap(GL_TEXTURE_2D);

		// free the image data from local memory
		stbi_image_free(image);
		glBindTexture(GL_TEXTURE_2D, 0); // Unbind the texture

		// register the loaded texture and associate it with the special tag string
		m_textureIDs[m_loadedTextures].ID = textureID;
		m_textureIDs[m_loadedTextures].tag = tag;
		m_loadedTextures++;

		return true;
	}

	std::cout << "Could not load image:" << filename << std::endl;

	// Error loading the image
	return false;
}

/***********************************************************
 *  BindGLTextures()
 *
 *  This method is used for binding the loaded textures to
 *  OpenGL texture memory slots.  There are up to 16 slots.
 ***********************************************************/
void SceneManager::BindGLTextures()
{
	for (int i = 0; i < m_loadedTextures; i++)
	{
		// bind textures on corresponding texture units
		glActiveTexture(GL_TEXTURE0 + i);
		glBindTexture(GL_TEXTURE_2D, m_textureIDs[i].ID);
	}
}

/***********************************************************
 *  DestroyGLTextures()
 *
 *  This method is used for freeing the memory in all the
 *  used texture memory slots.
 ***********************************************************/
void SceneManager::DestroyGLTextures()
{
	GLuint* textureIDs = new GLuint[m_loadedTextures];
	for (int i = 0; i < m_loadedTextures; i++)
	{
		glGenTextures(1, &m_textureIDs[i].ID);
	}

	glDeleteTextures(m_loadedTextures, textureIDs);

	delete[] textureIDs;
}

/***********************************************************
 *  FindTextureID()
 *
 *  This method is used for getting an ID for the previously
 *  loaded texture bitmap associated with the passed in tag.
 ***********************************************************/
int SceneManager::FindTextureID(std::string tag)
{
	int textureID = -1;
	int index = 0;
	bool bFound = false;

	while ((index < m_loadedTextures) && (bFound == false))
	{
		if (m_textureIDs[index].tag.compare(tag) == 0)
		{
			textureID = m_textureIDs[index].ID;
			bFound = true;
		}
		else
			index++;
	}

	return(textureID);
}

/***********************************************************
 *  FindTextureSlot()
 *
 *  This method is used for getting a slot index for the previously
 *  loaded texture bitmap associated with the passed in tag.
 ***********************************************************/
int SceneManager::FindTextureSlot(std::string tag)
{
	int textureSlot = -1;
	int index = 0;
	bool bFound = false;

	while ((index < m_loadedTextures) && (bFound == false))
	{
		if (m_textureIDs[index].tag.compare(tag) == 0)
		{
			textureSlot = index;
			bFound = true;
		}
		else
			index++;
	}

	return(textureSlot);
}

void SceneManager::DefineMaterials()
{
	//grass
	OBJECT_MATERIAL grassMaterial;
	grassMaterial.ambientStrength = 0.3f;
	grassMaterial.ambientColor = glm::vec3(0.13f, 0.55f, 0.13f);
	grassMaterial.diffuseColor = glm::vec3(0.13f, 0.55f, 0.13f);
	grassMaterial.specularColor = glm::vec3(0.1f, 0.1f, 0.1f);
	grassMaterial.shininess = 2.0f;
	grassMaterial.tag = "grass";
	m_objectMaterials.push_back(grassMaterial);

	//wood
	OBJECT_MATERIAL woodMaterial;
	woodMaterial.ambientStrength = 0.3f;
	woodMaterial.ambientColor = glm::vec3(0.8f, 0.7f, 0.5f);
	woodMaterial.diffuseColor = glm::vec3(0.8f, 0.7f, 0.5f);
	woodMaterial.specularColor = glm::vec3(0.3f, 0.3f, 0.3f);
	woodMaterial.shininess = 16.0f;
	woodMaterial.tag = "wood";
	m_objectMaterials.push_back(woodMaterial);

	//stone
	OBJECT_MATERIAL stoneMaterial;
	stoneMaterial.ambientStrength = 0.3f;
	stoneMaterial.ambientColor = glm::vec3(0.55f, 0.27f, 0.07f);
	stoneMaterial.diffuseColor = glm::vec3(0.55f, 0.27f, 0.07f);
	stoneMaterial.specularColor = glm::vec3(0.2f, 0.2f, 0.2f);
	stoneMaterial.shininess = 8.0f;
	stoneMaterial.tag = "stone";
	m_objectMaterials.push_back(stoneMaterial);

	//concrete
	OBJECT_MATERIAL concreteMaterial;
	concreteMaterial.ambientStrength = 0.3f;
	concreteMaterial.ambientColor = glm::vec3(0.5f, 0.5f, 0.5f);
	concreteMaterial.diffuseColor = glm::vec3(0.5f, 0.5f, 0.5f);
	concreteMaterial.specularColor = glm::vec3(0.1f, 0.1f, 0.1f);
	concreteMaterial.shininess = 4.0f;
	concreteMaterial.tag = "concrete";
	m_objectMaterials.push_back(concreteMaterial);

	//santas runway
	OBJECT_MATERIAL roofMaterial;
	roofMaterial.ambientStrength = 0.3f;
	roofMaterial.ambientColor = glm::vec3(0.3f, 0.3f, 0.35f);
	roofMaterial.diffuseColor = glm::vec3(0.3f, 0.3f, 0.35f);
	roofMaterial.specularColor = glm::vec3(0.2f, 0.2f, 0.2f);
	roofMaterial.shininess = 8.0f;
	roofMaterial.tag = "roof";
	m_objectMaterials.push_back(roofMaterial);

	//windows
	OBJECT_MATERIAL windowMaterial;
	windowMaterial.ambientStrength = 0.3f;
	windowMaterial.ambientColor = glm::vec3(0.1f, 0.1f, 0.1f);
	windowMaterial.diffuseColor = glm::vec3(0.2f, 0.2f, 0.3f);
	windowMaterial.specularColor = glm::vec3(0.8f, 0.8f, 0.9f);
	windowMaterial.shininess = 128.0f;
	windowMaterial.tag = "window";
	m_objectMaterials.push_back(windowMaterial);

	//door
	OBJECT_MATERIAL doorMaterial;
	doorMaterial.ambientStrength = 0.3f;
	doorMaterial.ambientColor = glm::vec3(0.4f, 0.2f, 0.1f);
	doorMaterial.diffuseColor = glm::vec3(0.4f, 0.2f, 0.1f);
	doorMaterial.specularColor = glm::vec3(0.3f, 0.3f, 0.3f);
	doorMaterial.shininess = 32.0f;
	doorMaterial.tag = "door";
	m_objectMaterials.push_back(doorMaterial);
}

/***********************************************************
 *  FindMaterial()
 *
 *  This method is used for getting a material from the previously
 *  defined materials list that is associated with the passed in tag.
 ***********************************************************/
bool SceneManager::FindMaterial(std::string tag, OBJECT_MATERIAL& material)
{
	if (m_objectMaterials.size() == 0)
	{
		return(false);
	}

	int index = 0;
	bool bFound = false;
	while ((index < m_objectMaterials.size()) && (bFound == false))
	{
		if (m_objectMaterials[index].tag.compare(tag) == 0)
		{
			bFound = true;
			material.ambientColor = m_objectMaterials[index].ambientColor;
			material.ambientStrength = m_objectMaterials[index].ambientStrength;
			material.diffuseColor = m_objectMaterials[index].diffuseColor;
			material.specularColor = m_objectMaterials[index].specularColor;
			material.shininess = m_objectMaterials[index].shininess;
		}
		else
		{
			index++;
		}
	}

	return(bFound);
}

//scenelights()
void SceneManager::SetupSceneLights()
{
	m_numLights = 0;

	//light 0, trying to replicate sun
	m_lights[0].type = 0;
	m_lights[0].direction = glm::normalize(glm::vec3(-0.7f, -0.5f, -0.5f));
	m_lights[0].ambientColor = glm::vec3(0.2f, 0.2f, 0.25f);  
	m_lights[0].diffuseColor = glm::vec3(0.9f, 0.85f, 0.7f); 
	m_lights[0].specularColor = glm::vec3(1.0f, 0.95f, 0.8f);
	m_lights[0].enabled = true;
	m_numLights++;

	//light 1, porch light
	m_lights[1].type = 1;  
	m_lights[1].position = glm::vec3(-0.5f, 3.5f, 3.5f);
	m_lights[1].ambientColor = glm::vec3(0.1f, 0.1f, 0.08f);
	m_lights[1].diffuseColor = glm::vec3(0.8f, 0.75f, 0.5f); 
	m_lights[1].specularColor = glm::vec3(0.9f, 0.85f, 0.7f);
	m_lights[1].constant = 1.0f;
	m_lights[1].linear = 0.09f;
	m_lights[1].quadratic = 0.032f;
	m_lights[1].enabled = true;
	m_numLights++;

	//garage light
	m_lights[2].type = 1;
	m_lights[2].position = glm::vec3(-4.0f, 3.0f, 4.5f);
	m_lights[2].ambientColor = glm::vec3(0.08f, 0.08f, 0.1f);
	m_lights[2].diffuseColor = glm::vec3(0.7f, 0.7f, 0.8f); 
	m_lights[2].specularColor = glm::vec3(0.8f, 0.8f, 0.9f);
	m_lights[2].constant = 1.0f;
	m_lights[2].linear = 0.07f;
	m_lights[2].quadratic = 0.017f;
	m_lights[2].enabled = true;
	m_numLights++;

	//light 3, filler light
	m_lights[3].type = 1;
	m_lights[3].position = glm::vec3(8.0f, 10.0f, 5.0f);
	m_lights[3].ambientColor = glm::vec3(0.15f, 0.15f, 0.2f);
	m_lights[3].diffuseColor = glm::vec3(0.4f, 0.4f, 0.5f);
	m_lights[3].specularColor = glm::vec3(0.2f, 0.2f, 0.3f);
	m_lights[3].constant = 1.0f;
	m_lights[3].linear = 0.014f;
	m_lights[3].quadratic = 0.0007f;
	m_lights[3].enabled = true;
	m_numLights++;
}

//light uniforms
void SceneManager::SetLightingUniforms()
{
	if (m_pShaderManager == nullptr) return;


	//pass num of active lights to shader
	m_pShaderManager->setIntValue("numLights", m_numLights);

	for (int i = 0; i < m_numLights && i < MAX_LIGHTS; i++)
	{
		std::string lightBase = "lights[" + std::to_string(i) + "].";

		//set light type 0,1,2 so on
		m_pShaderManager->setIntValue(lightBase + "type", m_lights[i].type);

		m_pShaderManager->setVec3Value(lightBase + "ambientColor", m_lights[i].ambientColor);
		m_pShaderManager->setVec3Value(lightBase + "diffuseColor", m_lights[i].diffuseColor);
		m_pShaderManager->setVec3Value(lightBase + "specularColor", m_lights[i].specularColor);
		m_pShaderManager->setBoolValue(lightBase + "enabled", m_lights[i].enabled);

		if (m_lights[i].type >= 1) 
		{
			m_pShaderManager->setVec3Value(lightBase + "position", m_lights[i].position);
			m_pShaderManager->setFloatValue(lightBase + "constant", m_lights[i].constant);
			m_pShaderManager->setFloatValue(lightBase + "linear", m_lights[i].linear);
			m_pShaderManager->setFloatValue(lightBase + "quadratic", m_lights[i].quadratic);
		}

		if (m_lights[i].type == 0 || m_lights[i].type == 2)  // Directional or spotlight
		{
			m_pShaderManager->setVec3Value(lightBase + "direction", m_lights[i].direction);
		}

		
		if (m_lights[i].type == 2) 
		{
			m_pShaderManager->setFloatValue(lightBase + "cutOff", m_lights[i].cutOff);
			m_pShaderManager->setFloatValue(lightBase + "outerCutOff", m_lights[i].outerCutOff);
		}
	}
}

//gotta enable lighting i suppose
void SceneManager::EnableLighting()
{
	if (m_pShaderManager)
	{
		m_pShaderManager->setBoolValue(g_UseLightingName, true);
		SetLightingUniforms();
	}
}

//need to turn off lights unless you want an $800 electric bill like mine
void SceneManager::DisableLighting()
{
	if (m_pShaderManager)
	{
		m_pShaderManager->setBoolValue(g_UseLightingName, false);
	}
}

/***********************************************************
 *  SetTransformations()
 *
 *  This method is used for setting the transform buffer
 *  using the passed in transformation values.
 ***********************************************************/
void SceneManager::SetTransformations(
	glm::vec3 scaleXYZ,
	float XrotationDegrees,
	float YrotationDegrees,
	float ZrotationDegrees,
	glm::vec3 positionXYZ)
{
	// variables for this method
	glm::mat4 modelView;
	glm::mat4 scale;
	glm::mat4 rotationX;
	glm::mat4 rotationY;
	glm::mat4 rotationZ;
	glm::mat4 translation;

	// set the scale value in the transform buffer
	scale = glm::scale(scaleXYZ);
	// set the rotation values in the transform buffer
	rotationX = glm::rotate(glm::radians(XrotationDegrees), glm::vec3(1.0f, 0.0f, 0.0f));
	rotationY = glm::rotate(glm::radians(YrotationDegrees), glm::vec3(0.0f, 1.0f, 0.0f));
	rotationZ = glm::rotate(glm::radians(ZrotationDegrees), glm::vec3(0.0f, 0.0f, 1.0f));
	// set the translation value in the transform buffer
	translation = glm::translate(positionXYZ);

	modelView = translation * rotationX * rotationY * rotationZ * scale;

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setMat4Value(g_ModelName, modelView);
	}
}

/***********************************************************
 *  SetShaderColor()
 *
 *  This method is used for setting the passed in color
 *  into the shader for the next draw command
 ***********************************************************/
void SceneManager::SetShaderColor(
	float redColorValue,
	float greenColorValue,
	float blueColorValue,
	float alphaValue)
{
	// variables for this method
	glm::vec4 currentColor;

	currentColor.r = redColorValue;
	currentColor.g = greenColorValue;
	currentColor.b = blueColorValue;
	currentColor.a = alphaValue;

	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setIntValue(g_UseTextureName, false);
		m_pShaderManager->setVec4Value(g_ColorValueName, currentColor);
	}
}

/***********************************************************
 *  SetShaderTexture()
 *
 *  This method is used for setting the texture data
 *  associated with the passed in ID into the shader.
 ***********************************************************/
void SceneManager::SetShaderTexture(
	std::string textureTag)
{
	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setIntValue(g_UseTextureName, true);

		int textureID = -1;
		textureID = FindTextureSlot(textureTag);
		m_pShaderManager->setSampler2DValue(g_TextureValueName, textureID);
	}
}

/***********************************************************
 *  SetTextureUVScale()
 *
 *  This method is used for setting the texture UV scale
 *  values into the shader.
 ***********************************************************/
void SceneManager::SetTextureUVScale(float u, float v)
{
	if (NULL != m_pShaderManager)
	{
		m_pShaderManager->setVec2Value("UVscale", glm::vec2(u, v));
	}
}

/***********************************************************
 *  SetShaderMaterial()
 *
 *  This method is used for passing the material values
 *  into the shader.
 ***********************************************************/
void SceneManager::SetShaderMaterial(
	std::string materialTag)
{
	if (m_objectMaterials.size() > 0)
	{
		OBJECT_MATERIAL material;
		bool bReturn = false;

		bReturn = FindMaterial(materialTag, material);
		if (bReturn == true)
		{
			m_pShaderManager->setVec3Value("material.ambientColor", material.ambientColor);
			m_pShaderManager->setFloatValue("material.ambientStrength", material.ambientStrength);
			m_pShaderManager->setVec3Value("material.diffuseColor", material.diffuseColor);
			m_pShaderManager->setVec3Value("material.specularColor", material.specularColor);
			m_pShaderManager->setFloatValue("material.shininess", material.shininess);
		}
	}
}

/**************************************************************/
/*** STUDENTS CAN MODIFY the code in the methods BELOW for  ***/
/*** preparing and rendering their own 3D replicated scenes.***/
/*** Please refer to the code in the OpenGL sample project  ***/
/*** for assistance.                                        ***/
/**************************************************************/


/***********************************************************
 *  PrepareScene()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void SceneManager::PrepareScene()
{
	// only one instance of a particular mesh needs to be
	// loaded in memory no matter how many times it is drawn
	// in the rendered 3D scene

	m_basicMeshes->LoadPlaneMesh();  //for ground and roof
	m_basicMeshes->LoadBoxMesh(); //for house main and garage
	m_basicMeshes->LoadPyramid3Mesh(); //for roof peaks
	m_basicMeshes->LoadCylinderMesh(); //for chimney
	m_basicMeshes->LoadPrismMesh(); //for roof

	m_loadedTextures = 0;

	//load images
	CreateGLTexture("../../Utilities/textures/grass.jpg", "grass");
	CreateGLTexture("../../Utilities/textures/wood.jpg", "wood");
	CreateGLTexture("../../Utilities/textures/stone.jpg", "stone");

	//bind texts
	BindGLTextures();

	//define mats for phong
	DefineMaterials();

	//set lights
	SetupSceneLights();
}

/***********************************************************
 *  RenderScene()
 *
 *  This method is used for rendering the 3D scene by 
 *  transforming and drawing the basic 3D shapes
 ***********************************************************/
void SceneManager::RenderScene()
{
	// declare the variables for the transformations
	glm::vec3 scaleXYZ;
	float XrotationDegrees = 0.0f;
	float YrotationDegrees = 0.0f;
	float ZrotationDegrees = 0.0f;
	glm::vec3 positionXYZ;

	glEnable(GL_DEPTH_TEST);
	glDepthFunc(GL_LEQUAL);

	//flip the switch
	EnableLighting();

	/*** Set needed transformations before drawing the basic mesh.  ***/
	/*** This same ordering of code should be used for transforming ***/
	/*** and drawing all the basic 3D shapes.						***/
	/******************************************************************/
	//create a large green yard
	scaleXYZ = glm::vec3(25.0f, 1.0f, 20.0f);

	// set the XYZ rotation for the yard
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;

	// set the XYZ position for the yard
	positionXYZ = glm::vec3(0.0f, 0.0f, 0.0f);

	// set the transformations into memory to be used on yard
	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ);

	//SetShaderColor(0.13f, 0.55f, 0.13f, 1.0f); //green for grass
	//set image instead of color
	SetShaderMaterial("grass");
	SetShaderTexture("grass");
	SetTextureUVScale(10.0f, 10.0f);

	// draw the yard with transformation values
	m_basicMeshes->DrawPlaneMesh();

	/******************************************************************/
	/*** driveway (Concrete/Gray)                                  ***/
	/******************************************************************/
	//create driveway plane - in front of garage
	/****************************************************************/
	scaleXYZ = glm::vec3(4.0f, 1.0f, 8.0f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-4.0f, 0.01f, 6.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//set grey color
	SetShaderColor(0.5f, 0.5f, 0.5f, 1.0f);
	//draw driveway
	m_basicMeshes->DrawPlaneMesh();

	/******************************************************************/
	/*** main house struct ***/
	/******************************************************************/
	//main house body - white upper section
	scaleXYZ = glm::vec3(8.0f, 4.0f, 6.0f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(2.0f, 2.0f, 0.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//set white siding color
	//SetShaderColor(0.95f, 0.95f, 0.95f, 1.0f);
	//wood siding instead
	SetShaderMaterial("wood");
	SetShaderTexture("wood");
	SetTextureUVScale(4.0f, 3.0f);
	
	m_basicMeshes->DrawBoxMesh();

	//going to try to add brick to the house
	scaleXYZ = glm::vec3(8.2f, 1.0f, 6.2f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(2.0f, 0.5f, 0.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//redish brick color
	//SetShaderColor(0.55f, 0.27f, 0.07f, 1.0f);

	//trying to add actual stone now
	SetShaderMaterial("stone");
	SetShaderTexture("stone");
	SetTextureUVScale(3.0f, 1.0f);

	m_basicMeshes->DrawBoxMesh();

	/******************************************************************/
	/*** garage on left side of house ***/
	/******************************************************************/
	//garage struct
	scaleXYZ = glm::vec3(5.0f, 3.5f, 5.0f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-4.0f, 1.75f, 1.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//white siding to match house
	//SetShaderColor(0.95f, 0.95f, 0.95f, 1.0f);
	SetShaderMaterial("wood");
	SetShaderTexture("wood");
	SetTextureUVScale(3.0f, 2.5f);

	m_basicMeshes->DrawBoxMesh();

	//garage brick
	scaleXYZ = glm::vec3(5.2f, 0.8f, 5.2f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-4.0f, 0.4f, 1.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//redish brick color
	//SetShaderColor(0.55f, 0.27f, 0.07f, 1.0f);
	SetShaderMaterial("stone");
	SetShaderTexture("stone");
	SetTextureUVScale(2.5f, 0.8f);
	
	m_basicMeshes->DrawBoxMesh();

	/******************************************************************/
	/***next up, garage door ***/
	/******************************************************************/
	
	scaleXYZ = glm::vec3(3.5f, 2.5f, 0.1f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-4.0f, 1.25f, 3.6f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//gray color for door
	SetShaderColor(0.9f, 0.9f, 0.9f, 1.0f);
	m_basicMeshes->DrawBoxMesh();


	/******************************************************************/
	/*** main roof using prism ***/
	/******************************************************************/
	//main house roof

	scaleXYZ = glm::vec3(8.5f, 2.0f, 7.0f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f; 
	positionXYZ = glm::vec3(2.0f, 4.5f, 0.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);
	//dark gray color for roof
	SetShaderColor(0.3f, 0.3f, 0.35f, 1.0f);
	m_basicMeshes->DrawPrismMesh();

	/******************************************************************/
	/***roof for garage now ***/
	/******************************************************************/
	scaleXYZ = glm::vec3(5.5f, 1.5f, 5.5f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 90.0f; //need to ratate to fit main roof
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-4.0f, 3.8f, 1.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	SetShaderColor(0.3f, 0.3f, 0.35f, 1.0f); //same as other roof
	m_basicMeshes->DrawPrismMesh();

	/******************************************************************/
	/***front door time ***/
	/******************************************************************/
	scaleXYZ = glm::vec3(0.8f, 2.0f, 0.1f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-0.5f, 1.5f, 3.1f);
	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);
	//brown door color
	SetShaderColor(0.4f, 0.2f, 0.1f, 1.0f);
	m_basicMeshes->DrawBoxMesh();

	/******************************************************************/
	/***windows because who doesnt love to get blinded by the sun or tv glare ***/
	/******************************************************************/
	//window 1, left of door
	scaleXYZ = glm::vec3(1.2f, 1.0f, 0.1f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(-1.0f, 2.0f, 3.1f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	//all windows will have a black frame
	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	m_basicMeshes->DrawBoxMesh();

	//window 2, upper floor on right side
	scaleXYZ = glm::vec3(1.5f, 1.0f, 0.1f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(3.5f, 3.0f, 3.1f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	m_basicMeshes->DrawBoxMesh();

	//window 3, larger window
	scaleXYZ = glm::vec3(2.0f, 1.5f, 0.1f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(5.0f, 2.0f, 3.1f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);

	SetShaderColor(0.1f, 0.1f, 0.1f, 1.0f);
	m_basicMeshes->DrawBoxMesh();

	/******************************************************************/
	/***going to try do a chimney, probably will fail  ***/
	/******************************************************************/
	scaleXYZ = glm::vec3(0.4f, 2.0f, 0.4f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(4.0f, 5.f, -1.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);
	//brick red for color of chimney
	//SetShaderColor(0.55f, 0.27f, 0.07f, 1.0f);
	SetShaderMaterial("stone");
	SetShaderTexture("stone");
	SetTextureUVScale(1.0f, 2.0f);

	m_basicMeshes->DrawCylinderMesh();

	/******************************************************************/
	/***sidewalk ***/
	/******************************************************************/
	scaleXYZ = glm::vec3(1.5f, 1.0f, 4.0f);
	XrotationDegrees = 0.0f;
	YrotationDegrees = 0.0f;
	ZrotationDegrees = 0.0f;
	positionXYZ = glm::vec3(0.5f, 0.01f, 5.0f);

	SetTransformations(
		scaleXYZ,
		XrotationDegrees,
		YrotationDegrees,
		ZrotationDegrees,
		positionXYZ
	);
	//light gray for sidewalk color
	SetShaderColor(0.6f, 0.6f, 0.6f, 1.0f);
	m_basicMeshes->DrawPlaneMesh();

}
