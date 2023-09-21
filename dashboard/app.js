// Define an array to store job card elements
const jobCards = [];
const socket = new WebSocket('ws://localhost:4567/api/logfirehose');

socket.addEventListener('message', async (event) => {
    const data = JSON.parse(event.data);

    // Assuming the WebSocket message format is { "jobId": "12345", "logLine": "Some log message" }
    const jobId = data.jobId;
    const logLine = data.logLine;

    // Check if a card for this job already exists; if not, create a new one.
    let jobCard = jobCards.find(card => card.id === jobId);

    if (!jobCard) {
        jobCard = createJobCard(jobId);
        jobCards.push(jobCard); // Add the job card to the array
        dashboard.appendChild(jobCard);
    }

    // Add log message to the log box of the job card.
    const logBox = jobCard.querySelector('.log-box');
    logBox.textContent += logLine + '\n'; // Use '\n' to add line breaks
    logBox.scrollTop = logBox.scrollHeight; // Auto-scroll to the bottom
});


function createJobCard(jobId) {
    const jobCard = document.createElement('div');
    jobCard.className = 'job-card';
    jobCard.id = jobId;

    // Fetch job information from the API (you may want to implement this).
    fetchJobInfo(jobId)
        .then((jobInfo) => {
            const { name, user } = jobInfo;

            // Create Job ID element.
            const jobIdElement = document.createElement('div');
            jobIdElement.className = 'job-id';
            jobIdElement.textContent = `${jobId}`;

            // Create job name and user elements.
            const nameElement = document.createElement('div');
            nameElement.className = 'job-name';
            nameElement.textContent = `${name}`;

            const userElement = document.createElement('div');
            userElement.className = 'job-user';
            userElement.textContent = `for ${user}`;

            // Create a log box for the job card.
            const logBox = document.createElement('div');
            logBox.className = 'log-box';
            logBox.textContent = ''; // Initialize log box content

            // Append elements to the job card.
            jobCard.appendChild(jobIdElement); // Add Job ID element
            jobCard.appendChild(nameElement);
            jobCard.appendChild(userElement);
            jobCard.appendChild(logBox); // Append the log box to the job card
        })
        .catch((error) => {
            console.error(error);
        });

    return jobCard;
}

// ...



async function fetchJobInfo(jobId) {
    // Implement the logic to fetch job information from your API here.
    // You can use the Fetch API or any other method that suits your server setup.
    // Return the job information as an object with "name" and "user" properties.
    const response = await fetch(`http://localhost:4567/api/jobs/${jobId}`);
    if (response.ok) {
        return response.json();
    } else {
        throw new Error(`Failed to fetch job information for Job ID: ${jobId}`);
    }
}
