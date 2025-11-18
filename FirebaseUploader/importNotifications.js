const admin = require('firebase-admin');
const serviceAccount = require('./serviceAccountKey.json');
const data = require('./notifications.json');

// Initialize Firebase Admin SDK
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function importNotifications() {
  const notifications = data.notifications;

  if (!notifications || notifications.length === 0) {
    console.log('No notifications to import.');
    return;
  }

  const collectionRef = db.collection('notifications');
  const batch = db.batch();

  console.log('Starting to import notifications...');

  for (const notification of notifications) {
    // Convert timestamp string to Firestore Timestamp object
    const firestoreTimestamp = admin.firestore.Timestamp.fromDate(new Date(notification.timestamp));
    
    const docRef = collectionRef.doc(); // Auto-generate document ID
    batch.set(docRef, {
      ...notification,
      timestamp: firestoreTimestamp
    });
  }

  try {
    await batch.commit();
    console.log(`Successfully imported ${notifications.length} notifications.`);
  } catch (error) {
    console.error('Error importing notifications: ', error);
  }
}

importNotifications();
