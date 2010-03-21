package com.badlogic.gdx.tests;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.RenderListener;
import com.badlogic.gdx.backends.desktop.JoglApplication;
import com.badlogic.gdx.graphics.FixedPointMesh;
import com.badlogic.gdx.graphics.Font;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.MeshRenderer;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Text;
import com.badlogic.gdx.graphics.Font.FontStyle;
import com.badlogic.gdx.math.Vector2D;

/**
 * A simple Pong remake showing how easy it is to quickly
 * prototype a game with libgdx.
 * 
 * @author mzechner
 *
 */
public class Pong implements RenderListener
{
	/** the camera **/
	private OrthographicCamera camera;
	/** the MeshRenderer for the paddles **/
	private MeshRenderer paddleMesh;
	/** the MeshRenderer for the ball **/
	private MeshRenderer ballMesh;
	/** the Font **/
	private Font font;
	/** the Text for displaying the score **/
	private Text score;
	
	/** the position of the two paddles **/
	private Vector2D leftPaddle = new Vector2D();
	private Vector2D rightPaddle = new Vector2D();
	/** the scores of the left and right paddle **/
	private int leftScore = 0;
	private int rightScore = 0;
	
	/** some constants **/
	private final int BALL_SPEED = 30;
	/** the position of the ball **/
	private Vector2D ball = new Vector2D();
	/** the ball direction **/
	private Vector2D ballDirection = new Vector2D();
	/** the current ball speed **/
	private int ballSpeed = BALL_SPEED;
	
	
	/**
	 * Here we setup all the resources. A {@link MeshRenderer}
	 * for the paddles which we use for both, a MeshRenderer 
	 * for the ball and a {@link Text} for rendering the score.
	 */
	@Override
	public void surfaceCreated(Application app) 
	{
		setupGraphics( app );
		setupGame( );
	}
	
	/**
	 * This method sets up all the graphics related stuff like the
	 * Meshes, the camera and the Font
	 * @param app
	 */
	private void setupGraphics( Application app )
	{
		//
		// We first construct the paddle mesh which consists of
		// four 2D vertices forming a vertically elongated rectangle
		// constructed around the origin. We don't use colors, normals
		// texture coordinates or indices. Note that we use a fixed
		// point Mesh here. The paddle has dimensions (10, 60).
		//
		Mesh mesh = new FixedPointMesh( 4, 2, false, false, false, 0, 0, false, 0 );
		mesh.setVertices( new float[] { -5, -30, 
										 5, -30, 
										 5,  30,
										-5,  30 } );
		paddleMesh = new MeshRenderer( app.getGraphics().getGL10(), mesh, true, true );
		
		// 
		// We do the same for the ball which has dimensions (10,10)
		//
		mesh = new FixedPointMesh( 4, 2, false, false, false, 0, 0, false, 0 );
		mesh.setVertices( new float[] { -5, -5,
										 5, -5,
										 5,  5,
										-5,  5 } );
		ballMesh = new MeshRenderer( app.getGraphics().getGL10(), mesh, true, true );
		
		//
		// We construct a new font from a system font. We assume
		// Arial is installed on both the desktop and Android.
		//
		font = app.getGraphics().newFont( "Arial", 30, FontStyle.Plain, true );
		score = font.newText();
		score.setText( "0 : 0" );
		
		//
		// Finally we construct an {@link OrthographicCamera} which
		// will scale our scene to 480x320 pixels no matter what the
		// real screen dimensions. This will of course squish the scene
		// on devices like the Droid. The screen center will be at (0,0)
		// so that's the reference frame for our scene.
		//
		camera = new OrthographicCamera( );
		camera.setViewport( 480, 320 );		
	}
	
	/**
	 * This sets up the game data like the initial
	 * paddle positions and the ball position and
	 * direction.
	 */
	private void setupGame( )
	{
		leftPaddle.set( -200, 20 );
		rightPaddle.set( 200, 0 );
		ball.set( 0, 0 );
		ballDirection.set( -1, 0 );
	}
	
	@Override
	public void dispose(Application app) 
	{
		
	}

	@Override
	public void render(Application app) 
	{
		// we update the game state so things move.
		updateGame(app);
		
		// First we clear the screen
		GL10 gl = app.getGraphics().getGL10();
		gl.glClear( GL10.GL_COLOR_BUFFER_BIT );
		
		// Next we update the camera and set the camera matrix
		camera.update();
		gl.glMatrixMode( GL10.GL_PROJECTION );
		gl.glLoadMatrixf( camera.getCombinedMatrix().val, 0 );
		gl.glMatrixMode( GL10.GL_MODELVIEW );
		gl.glLoadIdentity();
		
		// Now we render the ball, we remember that we
		// Defined 4 vertices so we use a triangle fan
		// We also push and pop the matrix. This is not really
		// necessary as the model view matrix doesn't contain
		// anything at this point.
		gl.glPushMatrix();
		gl.glTranslatef( ball.x, ball.y, 0 );
		ballMesh.render(GL10.GL_TRIANGLE_FAN);
		gl.glPopMatrix();
		
		// Rendering the two paddles works analogous
		gl.glPushMatrix();
		gl.glTranslatef( leftPaddle.x, leftPaddle.y, 0 );
		paddleMesh.render(GL10.GL_TRIANGLE_FAN);
		gl.glPopMatrix();
		
		gl.glPushMatrix();
		gl.glTranslatef( rightPaddle.x, rightPaddle.y, 0 );
		paddleMesh.render(GL10.GL_TRIANGLE_FAN);
		gl.glPopMatrix();
		
		// Finally we render the text centered at the top
		// of the screen. We use the text bounds for this.
		// For text to be transparent we have to enable blending.
		// We could setup blending once but i'm lazy :)
		gl.glEnable( GL10.GL_BLEND );
		gl.glBlendFunc( GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA );
		gl.glPushMatrix();
		gl.glTranslatef( -score.getWidth() / 2, 160 - score.getHeight(), 0 );
//		score.render();
		gl.glPopMatrix();
		gl.glDisable( GL10.GL_BLEND );
	}

	/**
	 * Updates the game state, moves the ball,
	 * checks for collisions or whether the ball
	 * has left the playfield. 
	 * 
	 * @param deltaTime the time elapsed since the last frame
	 */
	private void updateGame( Application app ) 
	{
		// the delta time so we can do frame independant time based movement
		float deltaTime = app.getGraphics().getDeltaTime();
		
		// move the ball with a velocity of 50 pixels
		// per second. The ballDirection is a unit vector
		// we simply scale by the velocity.
		ball.add( ballSpeed * ballDirection.x * deltaTime, ballSpeed * ballDirection.y * deltaTime );
		
		// Next we check wheter the ball left the field to
		// the left or to the right and update the score
		if( ball.x < -240 )
		{
			ball.set( 0, 0 ); // reset to center
			ballSpeed = BALL_SPEED; // reset the ball speed 
			ballDirection.set( (float)Math.random() + 0.1f, (float)Math.random() ).nor(); // new ball direction, must be unit length
			rightScore++;     // right paddle scored!
		}
		
		if( ball.x > 240 )
		{
			ball.set( 0, 0 ); // reset to center
			ballDirection.set( (float)Math.random() + 0.1f, (float)Math.random() ).nor(); // new ball direction, must be unit length
			leftScore++; 	  // left paddle scored!
		}
		
		// if the ball is hitting the bottom or top we
		// reverse its direction in y so that it bounces
		if( ball.y > 160 || ball.y < -160 )
			ballDirection.y = -ballDirection.y;
		
		// if the ball is heading towards the right paddle and
		// has hit it we reflect it
		if( ballDirection.x > 0 && ball.x > rightPaddle.x - 5 &&
		    ball.y > rightPaddle.y - 30 && ball.y < rightPaddle.y + 30 )
		{
			ball.x = rightPaddle.x - 6; // set the position of a little so we don't get to this code in the next frame
			ballDirection.x = -ballDirection.x;
			float sign = Math.signum(ball.y - rightPaddle.y);
			ballDirection.y = sign * (float)Math.abs(ball.y - rightPaddle.y) / 30;  // reflect it depending on where the paddle was hit
			ballDirection.nor();
			ballSpeed += 10; // and faster!
		}
		
		// and the same for the left paddle
		if( ballDirection.x < 0 && ball.x < leftPaddle.x + 5 &&
			ball.y > leftPaddle.y - 30 && ball.y < leftPaddle.y + 30 )
		{
			ball.x = leftPaddle.x + 6; // set the position of a little so we don't get to this code in the next frame
			ballDirection.x = -ballDirection.x;
			float sign =(float)Math.signum(ball.y - leftPaddle.y);
			ballDirection.y = sign * (float)Math.abs(ball.y - leftPaddle.y) / 30;  // reflect it depending on where the paddle was hit
			ballDirection.nor();
			ballSpeed += 10; // and faster!
		}
		
		// Has the user touched the screen? then position the paddle
		if( app.getInput().isTouched() )
		{
			// get the touch coordinates and translate them
			// to the game coordinate system.
			float touchX = 480 * (app.getInput().getX() / app.getGraphics().getWidth() - 0.5f);
			float touchY = 320 * (0.5f - app.getInput().getY() / app.getGraphics().getHeight());
			
			if( touchX > leftPaddle.x )
				ball.y = touchY;
		}
	}

	@Override
	public void surfaceChanged(Application app, int width, int height) 
	{
		
	}
	
	/**
	 * The main method for the desktop version of Pong.
	 * @param argv
	 */
	public static void main( String[] argv )
	{
		// we create a new JoglApplication and register a new Pong instances as the RenderListener
		JoglApplication app = new JoglApplication( "Pong", 480, 320, false );
		app.getGraphics().setRenderListener( new Pong() );
	}
}
