document.querySelectorAll('.print-ticket').forEach(button => {
    button.addEventListener('click', function (event) {
        event.preventDefault(); // Prevent default link behavior
        const bookingId = this.getAttribute('data-booking-id'); // Get the booking ID from the button
        fetch(`/api/admin/ticket/generate-pdfs/${bookingId}`).then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json(); // Parse the JSON response
        }).then(urls => {
            if (!Array.isArray(urls)) {
                throw new Error('Response is not an array');
            }
            // Open each PDF for viewing and trigger download separately
            urls.forEach((url, index) => {
                // Open the PDF in a new tab
                setTimeout(() => {
                    const viewUrl = url.replace('/download/', '/view/');
                    window.open(viewUrl, '_blank');
                }, index * 500); // Delay each tab opening by 500ms
                // Trigger download after a short delay
                setTimeout(() => {
                    const downloadLink = document.createElement('a');
                    downloadLink.href = url;
                    downloadLink.download = ''; // Trigger download without needing to specify filename
                    document.body.appendChild(downloadLink);
                    downloadLink.click();
                    document.body.removeChild(downloadLink);
                }, (index * 500) + 1000); // Delay download by 1 second after opening tab
            });
        }).catch(error => console.error('Error:', error)); // Log errors to the console
    });
});