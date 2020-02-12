import java.util.concurrent.Semaphore;

import TSim.*;

public class Lab1 {
	TSimInterface tsi;
	private Semaphore toppKorsning = new Semaphore(1);
	private Semaphore topRight = new Semaphore(1);
	private Semaphore topTop = new Semaphore(1);
	private Semaphore middleMid = new Semaphore(1);
	private Semaphore bottomLeft = new Semaphore(1);
	private Semaphore bottomDefault = new Semaphore(1);
	public static final int ACTIVE = 0x01;
	public static final int INACTIVE = 0x02;

	public class TrainRunner implements Runnable {
		int speed, trainId;
		boolean dirDown;

		public TrainRunner(int speed, int trainId, boolean dirDown) {
			this.speed = speed;
			this.trainId = trainId;
			this.dirDown = dirDown;
		}
		//gets a semaphore, trainId and speed. Stops the train until it acquires the semaphore then starts it again with its old speed. 
		public void trainWait(Semaphore s, int trainId, int speed) {
			try {
				tsi.setSpeed(trainId, 0);
				s.acquire();
				tsi.setSpeed(trainId, speed);
			} catch (CommandException e2) {
				e2.printStackTrace();

			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
		//stops the train at the station, sleep the thread for a while then reverses the train with the same speed
		public int stationStop(int trainId, int speed) {
			try {
				tsi.setSpeed(trainId, 0);
				Thread.sleep(1000 + (20 * Math.abs(speed)));
				speed = -speed;
				tsi.setSpeed(trainId, speed);
			} catch (CommandException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return speed;
		}

		@Override
		public void run() {
			try {
				SensorEvent se = null;
				tsi.setSpeed(trainId, speed);

				while (true) {
					//get the sensor event and then convert the x and y coordinates to a string for our switch
					se = tsi.getSensor(trainId);
					String pos = String.valueOf(se.getXpos()) + "," + String.valueOf(se.getYpos());
					if (se.getTrainId() == trainId && se.getStatus() == ACTIVE) {
						switch (pos) {
						case "6,7": {
							if (dirDown) {
								trainWait(toppKorsning, trainId, speed);
								break;
							}
							break;
						}
						case "10,7": {
							if (!dirDown)
								trainWait(toppKorsning, trainId, speed);
							break;
						}
						case "8,5": {
							if (dirDown)
								trainWait(toppKorsning, trainId, speed);
							break;
						}
						case "9,8": {
							if (!dirDown)
								trainWait(toppKorsning, trainId, speed);
							break;
						}
						case "15,7": {
							if (dirDown) {
								trainWait(topRight, trainId, speed);
								tsi.setSwitch(17, 7, tsi.SWITCH_RIGHT);
							}
							break;
						}
						case "15,8": {
							if (dirDown) {
								trainWait(topRight, trainId, speed);
								tsi.setSwitch(17, 7, tsi.SWITCH_LEFT);
							}
							break;
						}
						case "19,7": {
							if (!dirDown) {
								if (topTop.tryAcquire()) {
									tsi.setSwitch(17, 7, tsi.SWITCH_RIGHT);
									break;
								}
								tsi.setSwitch(17, 7, tsi.SWITCH_LEFT);
							}
							break;
						}
						case "17,9": {
							if (dirDown) {
								if (middleMid.tryAcquire()) {
									tsi.setSwitch(15, 9, tsi.SWITCH_RIGHT);
									break;
								}
								tsi.setSwitch(15, 9, tsi.SWITCH_LEFT);
							}
							break;
						}
						case "13,9": {
							if (!dirDown) {
								trainWait(topRight, trainId, speed);
								tsi.setSwitch(15, 9, tsi.SWITCH_RIGHT);
							}
							break;
						}
						case "13,10": {
							if (!dirDown) {
								trainWait(topRight, trainId, speed);
								tsi.setSwitch(15, 9, tsi.SWITCH_LEFT);
							}
							break;
						}
						case "6,9": {
							if (dirDown) {
								trainWait(bottomLeft, trainId, speed);
								tsi.setSwitch(4, 9, tsi.SWITCH_LEFT);
							}
							break;
						}
						case "6,10": {
							if (dirDown) {
								trainWait(bottomLeft, trainId, speed);
								tsi.setSwitch(4, 9, tsi.SWITCH_RIGHT);
							}
							break;
						}
						case "2,9": {

							if (!dirDown) {
								if (middleMid.tryAcquire()) {
									tsi.setSwitch(4, 9, tsi.SWITCH_LEFT);
									break;
								}
								tsi.setSwitch(4, 9, tsi.SWITCH_RIGHT);
							}
							break;
						}
						case "1,11": {
							if (dirDown) {
								if (bottomDefault.tryAcquire()) {
									tsi.setSwitch(3, 11, tsi.SWITCH_LEFT);
									break;
								}
								tsi.setSwitch(3, 11, tsi.SWITCH_RIGHT);
							}
							break;
						}
						case "5,11": {
							if (!dirDown) {
								trainWait(bottomLeft, trainId, speed);
								tsi.setSwitch(3, 11, tsi.SWITCH_LEFT);
							}
							break;
						}
						case "3,13": {
							if (!dirDown) {
								trainWait(bottomLeft, trainId, speed);
								tsi.setSwitch(3, 11, tsi.SWITCH_RIGHT);
							}
							break;
						}
						case "14,11": {
							if (dirDown) {
								speed = stationStop(trainId, speed);
								dirDown = false;
							} else
								bottomDefault.acquire();
							break;
						}
						case "14,13": {
							speed = stationStop(trainId, speed);
							dirDown = false;
							break;
						}
						case "14,3": {
							if (!dirDown) {
								speed = stationStop(trainId, speed);
								dirDown = true;
							} else
								topTop.acquire();
							break;
						}
						case "15,5": {
							speed = stationStop(trainId, speed);
							dirDown = true;
							break;
						}
						}
					}
					if (se.getTrainId() == trainId && se.getStatus() == INACTIVE) {

						switch (pos) {
						case "13,10": {
							if (dirDown)
								topRight.release();
							break;
						}
						case "5,11": {
							if (!dirDown)
								bottomDefault.release();
							else 
								bottomLeft.release();
							break;
						}
						case "3,13": {
							if (dirDown)
								bottomLeft.release();
							break;
						}
						case "15,7": {
							if (dirDown) 
								topTop.release();
							else 
								topRight.release();
							break;
						}
						case "15,8": {
							if (!dirDown) 
								topRight.release();
							break;
						}
						case "6,9": {
							if (dirDown) 
								middleMid.release();
							else
								bottomLeft.release();
							break;
						}
						case "6,10": {
							if (!dirDown)
								bottomLeft.release();
							break;
						}
						case "13,9": {
							if (dirDown) 
								topRight.release(); 
							else
								middleMid.release();
							break;
						}
						case "6,7": {
							if (!dirDown)
								toppKorsning.release();
							break;
						}
						case "8,5": {
							if (!dirDown)
								toppKorsning.release();
							break;
						}
						case "10,7": {
							if (dirDown)
								toppKorsning.release();
							break;

						}
						case "9,8": {
							if (dirDown)
								toppKorsning.release();
							break;
						}
						}

					}
				}
			} catch (CommandException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public Lab1(int speed1, int speed2) {
		tsi = TSimInterface.getInstance();
		//creates 2 threads and send the variables we need
		Thread t1 = new Thread(new TrainRunner(speed1, 1, true));
		Thread t2 = new Thread(new TrainRunner(speed2, 2, false));
		t1.start();
		t2.start();
	}
}
