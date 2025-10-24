///////////////////////////////////////////////////////////////////////////////
// viewmanager.h
// ============
// manage the viewing of 3D objects within the viewport
//
//  AUTHOR: Brian Battersby - SNHU Instructor / Computer Science
//	Created for CS-330-Computational Graphics and Visualization, Nov. 1st, 2023
///////////////////////////////////////////////////////////////////////////////

#include "ViewManager.h"
#include "Camera.h"

// GLM Math Header inclusions
#include <glm/glm.hpp>
#include <glm/gtx/transform.hpp>
#include <glm/gtc/type_ptr.hpp>    

// declaration of the global variables and defines
namespace
{
	// Variables for window width and height
	const int WINDOW_WIDTH = 1000;
	const int WINDOW_HEIGHT = 800;
	const char* g_ViewName = "view";
	const char* g_ProjectionName = "projection";

	// camera object used for viewing and interacting with
	// the 3D scene
	Camera* g_pCamera = nullptr;

	// these variables are used for mouse movement processing
	float gLastX = WINDOW_WIDTH / 2.0f;
	float gLastY = WINDOW_HEIGHT / 2.0f;
	bool gFirstMouse = true;

	// time between current frame and last frame
	float gDeltaTime = 0.0f; 
	float gLastFrame = 0.0f;

	//cam move speed
	float gCameraSpeed = 2.5f;

	//mouse sensitivity
	float gMouseSensitivity = 0.1f;

	// the following variable is false when orthographic projection
	// is off and true when it is on
	bool bOrthographicProjection = false;
}

/***********************************************************
 *  ViewManager()
 *
 *  The constructor for the class
 ***********************************************************/
ViewManager::ViewManager(
	ShaderManager *pShaderManager)
{
	// initialize the member variables
	m_pShaderManager = pShaderManager;
	m_pWindow = NULL;
	g_pCamera = new Camera();
	// default camera view parameters
	g_pCamera->Position = glm::vec3(10.0f, 8.0f, 15.0f);
	g_pCamera->Front = glm::vec3(-0.5f, -0.3f, -0.7f);
	g_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
	g_pCamera->Zoom = 45.0f;
	g_pCamera->Pitch = -20.0f;
	g_pCamera->Yaw = -120.0f;
	g_pCamera->updateCameraVectors();
}

/***********************************************************
 *  ~ViewManager()
 *
 *  The destructor for the class
 ***********************************************************/
ViewManager::~ViewManager()
{
	// free up allocated memory
	m_pShaderManager = NULL;
	m_pWindow = NULL;
	if (NULL != g_pCamera)
	{
		delete g_pCamera;
		g_pCamera = NULL;
	}
}

/***********************************************************
 *  CreateDisplayWindow()
 *
 *  This method is used to create the main display window.
 ***********************************************************/
GLFWwindow* ViewManager::CreateDisplayWindow(const char* windowTitle)
{
	GLFWwindow* window = nullptr;

	// try to create the displayed OpenGL window
	window = glfwCreateWindow(
		WINDOW_WIDTH,
		WINDOW_HEIGHT,
		windowTitle,
		NULL, NULL);
	if (window == NULL)
	{
		std::cout << "Failed to create GLFW window" << std::endl;
		glfwTerminate();
		return NULL;
	}
	glfwMakeContextCurrent(window);

	// tell GLFW to capture all mouse events
	glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);

	// this callback is used to receive mouse moving events
	glfwSetCursorPosCallback(window, &ViewManager::Mouse_Position_Callback);
	glfwSetScrollCallback(window, &ViewManager::Mouse_Scroll_Callback);

	// enable blending for supporting tranparent rendering
	glEnable(GL_BLEND);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

	m_pWindow = window;

	return(window);
}

/***********************************************************
 *  Mouse_Position_Callback()
 *
 *  This method is automatically called from GLFW whenever
 *  the mouse is moved within the active GLFW display window.
 ***********************************************************/
void ViewManager::Mouse_Position_Callback(GLFWwindow* window, double xMousePos, double yMousePos)
{
	//prevent cam jump
	if (gFirstMouse)
	{
		gLastX = xMousePos;
		gLastY = yMousePos;
		gFirstMouse = false;
	}

	//calc mouse offset
	float xOffset = xMousePos - gLastX;
	float yOffset = gLastY - yMousePos;

	//last mouse pos
	gLastX = xMousePos;
	gLastY = yMousePos;

	//mouse sensitivity
	xOffset *= gMouseSensitivity;
	yOffset *= gMouseSensitivity;

	//update camera
	g_pCamera->Pitch += yOffset;
	g_pCamera->Yaw += xOffset;

	//prevent cam flip
	if (g_pCamera->Pitch > 89.0f)
		g_pCamera->Pitch = 89.0f;
	if (g_pCamera->Pitch < -89.0f)
		g_pCamera->Pitch = -89.0f;

	g_pCamera->updateCameraVectors();
}

/***********************************************************
 *  Mouse_Scroll_Callback()
 *
 *  this is automatically called from GLFW whenever
 *  the mouse scroll wheel is used, it adjusts cam speediness.
 ***********************************************************/

void ViewManager::Mouse_Scroll_Callback(GLFWwindow* window, double xOffset, double yOffset)
{
	//adjust speediness
	gCameraSpeed += (float)yOffset * 0.5f;

	if (gCameraSpeed < 0.5f)
		gCameraSpeed = 0.5f;
	if (gCameraSpeed > 10.0f)
		gCameraSpeed = 10.0f;

	//console to make sure it works
	std::cout << "cam speed: " << gCameraSpeed << std::endl;
}

/***********************************************************
 *  ProcessKeyboardEvents()
 *
 *  This method is called to process any keyboard events
 *  that may be waiting in the event queue.
 ***********************************************************/
void ViewManager::ProcessKeyboardEvents()
{
	//calc velocity for smooth
	float velocity = gCameraSpeed * gDeltaTime;

	// close the window if the escape key has been pressed
	if (glfwGetKey(m_pWindow, GLFW_KEY_ESCAPE) == GLFW_PRESS)
	{
		glfwSetWindowShouldClose(m_pWindow, true);
	}
	/******************************************************************/
	/*** WASD CAM MOVEMENT - Forward, Backward, Left, Right     ***/
	/******************************************************************/

	//w key, move forward
	if (glfwGetKey(m_pWindow, GLFW_KEY_W) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(FORWARD, velocity);
	}

	//s key, move back
	if (glfwGetKey(m_pWindow, GLFW_KEY_S) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(BACKWARD, velocity);
	}

	//a key, slide to the left
	if (glfwGetKey(m_pWindow, GLFW_KEY_A) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(LEFT, velocity);
	}

	//d key, slide to the right
	if (glfwGetKey(m_pWindow, GLFW_KEY_D) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(RIGHT, velocity);
	}

	//q,e movement
	if (glfwGetKey(m_pWindow, GLFW_KEY_Q) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(UP, velocity);
	}

	if (glfwGetKey(m_pWindow, GLFW_KEY_E) == GLFW_PRESS)
	{
		g_pCamera->ProcessKeyboard(DOWN, velocity);
	}

	//switch perspective key = p
	static bool pKeyPressed = false;
	if (glfwGetKey(m_pWindow, GLFW_KEY_P) == GLFW_PRESS && !pKeyPressed)
	{
		bOrthographicProjection = false;
		pKeyPressed = true;
		//log switch
		std::cout << "switched perspective" << std::endl;
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_P) == GLFW_RELEASE)
	{
		pKeyPressed = false;
	}
	//switch ortho
	static bool oKeyPressed = false;
	if (glfwGetKey(m_pWindow, GLFW_KEY_O) == GLFW_PRESS && !oKeyPressed)
	{
		bOrthographicProjection = true;
		oKeyPressed = true;
		std::cout << "Switched to Orthographic Projection" << std::endl;

		if (bOrthographicProjection)
		{
			g_pCamera->Position = glm::vec3(0.0f, 20.0f, 0.1f);
			g_pCamera->Front = glm::vec3(0.0f, -1.0f, 0.0f);
			g_pCamera->Up = glm::vec3(0.0f, 0.0f, -1.0f);
			g_pCamera->Pitch = -89.0f;
			g_pCamera->Yaw = -90.0f;
			g_pCamera->updateCameraVectors();
		}
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_O) == GLFW_RELEASE)
	{
		oKeyPressed = false;
	}

	//reset cam, key = r
	static bool rKeyPressed = false;
	if (glfwGetKey(m_pWindow, GLFW_KEY_R) == GLFW_PRESS && !rKeyPressed)
	{
		rKeyPressed = true;
		//reset
		g_pCamera->Position = glm::vec3(10.0f, 8.0f, 15.0f);
		g_pCamera->Front = glm::vec3(-0.5f, -0.3f, -0.7f);
		g_pCamera->Up = glm::vec3(0.0f, 1.0f, 0.0f);
		g_pCamera->Yaw = -120.0f;
		g_pCamera->Pitch = -20.0f;
		g_pCamera->updateCameraVectors();
		bOrthographicProjection = false;
		std::cout << "Camera Reset to Default Position" << std::endl;
	}
	if (glfwGetKey(m_pWindow, GLFW_KEY_R) == GLFW_RELEASE)
	{
		rKeyPressed = false;
	}
}

/***********************************************************
 *  PrepareSceneView()
 *
 *  This method is used for preparing the 3D scene by loading
 *  the shapes, textures in memory to support the 3D scene 
 *  rendering
 ***********************************************************/
void ViewManager::PrepareSceneView()
{
	glm::mat4 view;
	glm::mat4 projection;

	// per-frame timing
	float currentFrame = glfwGetTime();
	gDeltaTime = currentFrame - gLastFrame;
	gLastFrame = currentFrame;

	// process any keyboard events that may be waiting in the 
	// event queue
	ProcessKeyboardEvents();

	// get the current view matrix from the camera
	view = g_pCamera->GetViewMatrix();

	// define the current projection matrix
	if (bOrthographicProjection)
	{
		//2d
		float orthoSize = 15.0f;
		float aspectRatio = (float)WINDOW_WIDTH / (float)WINDOW_HEIGHT;

		projection = glm::ortho(
			//left
			-orthoSize * aspectRatio,
			//right
			orthoSize * aspectRatio,
			//bottom
			-orthoSize,
			//top
			orthoSize,
			//near plane
			0.1f,
			//far plane
			100.0f
		);
	}
	else
	{
		projection = glm::perspective(glm::radians(g_pCamera->Zoom), (GLfloat)WINDOW_WIDTH / (GLfloat)WINDOW_HEIGHT, 0.1f, 100.0f);
	}

	// if the shader manager object is valid
	if (NULL != m_pShaderManager)
	{
		// set the view matrix into the shader for proper rendering
		m_pShaderManager->setMat4Value(g_ViewName, view);
		// set the view matrix into the shader for proper rendering
		m_pShaderManager->setMat4Value(g_ProjectionName, projection);
		// set the view position of the camera into the shader for proper rendering
		m_pShaderManager->setVec3Value("viewPosition", g_pCamera->Position);
	}
}