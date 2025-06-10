import aiohttp
import asyncio
import time

# Server URL
webserver_ip = "localhost"  # Change to your server's IP if needed
server_url = f"http://{webserver_ip}:8000/capturetheflag?gridSize=30&numBlueAgents=29&numRedAgents=29&flagPlacementType=A"

# Function to make a single async request
async def make_request(session, url):
    start = time.perf_counter()
    try:
        async with session.get(url) as response:
            content = await response.text()
            end = time.perf_counter()
            elapsed = end - start
            return {
                'url': url,
                'status': response.status,
                'time': elapsed,
                'content': content,
            }
    except Exception as e:
        end = time.perf_counter()
        return {
            'url': url,
            'status': 'ERROR',
            'time': end - start,
            'content': str(e),
        }

# Main function to make multiple concurrent requests
async def main():
    async with aiohttp.ClientSession() as session:
        tasks = [make_request(session, server_url) for _ in range(2)]  # Adjust number of requests here
        responses = await asyncio.gather(*tasks)
        for i, response in enumerate(responses):
            print(f"\n--- Response {i + 1} ---")
            print(f"Status: {response['status']}")
            print(f"Time: {response['time']:.2f} seconds")
            print(f"Content:\n{response['content']}\n")

# Entry point
if __name__ == "__main__":
    asyncio.run(main())
