package org.example.asteroides;

import java.util.Vector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class VistaJuego extends View {

	// Nave //
	private Grafico nave; // grafico de la nave
	private int giroNave; // incremento de dirección
	private float aceleracionNave; // aumento de velocidad

	// Asteroides //
	private Vector<Grafico> Asteroides;
	private int numAsteroides = 5; // numero inicial de asteroides
	private int numFragmentos = 3; // numero incial de fragmento

	// variables touch //
	private float mX = 0, mY = 0;
	private boolean disparo = false;

	private static final int PASO_GIRO_NAVE = 5;
	private static final float PASO_ACELERACION_NAVE = 0.5f;

	// thread encargado de procesar el juego
	private ThreadJuego thread = new ThreadJuego();

	// cada cuanto queremos procesar cambios (50ms)
	private static int PERIODO_PROCESO = 50;

	// cuando se realizo el ultimo proceso
	private long ultimoProceso = 0;

	public VistaJuego(Context context, AttributeSet attrs) {
		super(context, attrs);
		Drawable drawableNave, drawableAsteroide, drawableMisil;
		drawableAsteroide = context.getResources().getDrawable(
				R.drawable.asteroide1);
		drawableNave = context.getResources().getDrawable(R.drawable.nave);
		nave = new Grafico(this, drawableNave);

		Asteroides = new Vector<Grafico>();

		for (int i = 0; i < numAsteroides; i++) {
			Grafico asteroide = new Grafico(this, drawableAsteroide);
			asteroide.setIncY(Math.random() * 4 - 2);
			asteroide.setIncX(Math.random() * 4 - 2);
			asteroide.setAngulo((int) (Math.random() * 360));
			asteroide.setRotacion((int) (Math.random() * 8 - 4));

			Asteroides.add(asteroide);
		}
	}

	@Override
	protected void onSizeChanged(int ancho, int alto, int anchoAnter,
			int altoAnter) {
		super.onSizeChanged(alto, ancho, altoAnter, anchoAnter);
		for (Grafico asteroide : Asteroides) {
			asteroide.setPosX(Math.random() * (ancho - asteroide.getAncho()));
			asteroide.setPosY(Math.random() * (alto - asteroide.getAlto()));
			nave.setPosX((ancho - nave.getAncho()) / 2);
			nave.setPosY((alto - nave.getAlto()) / 2);

			ultimoProceso = System.currentTimeMillis();
			if (thread.getState() == Thread.State.NEW)
				thread.start();
		}
	}

	@Override
	synchronized protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		for (Grafico asteroide : Asteroides) {
			asteroide.dibujarGrafico(canvas);
		}
		nave.dibujarGrafico(canvas);
	}

	synchronized protected void actualizaFisica() {
		long ahora = System.currentTimeMillis();
		// no hagas nada si el periodo de proceso no se ha cumplido
		if (ultimoProceso + PERIODO_PROCESO > ahora) {
			return;
		}
		// para una ejecución en tiempo real, calculamos el resultado
		double retardo = (ahora - ultimoProceso) / PERIODO_PROCESO;
		ultimoProceso = ahora; // para la próxima vez

		// Actualizamos velocidad y direccion de la nave a partir de
		// giroNave y aceleracionNave (segun la entrada del jugador)
		nave.setAngulo((int) (nave.getAngulo() + giroNave * retardo));

		double nIncX = nave.getIncX() + aceleracionNave
				* Math.cos(Math.toRadians(nave.getAngulo())) * retardo;
		double nIncY = nave.getIncY() + aceleracionNave
				* Math.sin(Math.toRadians(nave.getAngulo())) * retardo;

		// Actualizamos si el módulo de la velocidad no excede el máximo
		if (Math.hypot(nIncX, nIncY) <= Grafico.MAX_VELOCIDAD) {
			nave.setIncX(nIncX);
			nave.setIncY(nIncY);
		}
		// Actualizamos posiciones X e Y
		nave.incrementaPos(retardo);
		for (Grafico asteroide : Asteroides) {
			asteroide.incrementaPos(retardo);
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		super.onTouchEvent(event);
		float x = event.getX();
		float y = event.getY();
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			disparo = true;
			break;
		case MotionEvent.ACTION_MOVE:
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);

			if (dy < 2 && dx > 2) {
				giroNave = Math.round(x - mX);
				disparo = false;
			} else if (dx < 2 && dy > 2) {
				if (mY > y) {
					aceleracionNave = Math.round(mY - y) / 25;
				}
				disparo = false;
			}
			break;
		case MotionEvent.ACTION_UP:
			giroNave = 0;
			aceleracionNave = 0;
			if (disparo) {
				// activaMisil();
			}
			break;
		}
		mX = x;
		mY = y;
		return true;
	}

	class ThreadJuego extends Thread {
		@Override
		public void run() {
			while (true) {
				actualizaFisica();
			}
		}
	}
}
