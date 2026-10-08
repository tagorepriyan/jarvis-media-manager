import requests

def send_text_message(token, chat_id, message, disable_notification=False):
    '''
    Sends a text message to a specific Telegram chat.
    '''
    if not token:
        raise ValueError('A bot token is required')
    if not message or not message.strip():
        raise ValueError('Message text cannot be empty')

    # Construct the URL using the bot token
    url = f'https://api.telegram.org/bot{token}/sendMessage'

    # Define the payload
    data = {
        'chat_id': chat_id,
        'text': message,
        # Optional: Use 'Markdown' or 'HTML' to style your message
        'parse_mode': 'Markdown',
        # Optional: Send silently if True
        'disable_notification': disable_notification
    }

    # Send the POST request
    response = requests.post(url=url, data=data, timeout=30)
    response.raise_for_status()

    return response

# Usage example
# Replace 'your_token_here' with the token from BotFather
# Replace 123456789 with your actual user ID (you can find this via @userinfobot)
