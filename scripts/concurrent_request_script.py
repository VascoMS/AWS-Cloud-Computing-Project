import aiohttp
import asyncio
import time


server_url = "http://34.207.141.3:8000/capturetheflag?gridSize=30&numBlueAgents=29&numRedAgents=29&flagPlacementType=A"

async def make_request(session, url):
    start = time.perf_counter()
    async with session.get(url) as response:
        content = await response.text()
        end = time.perf_counter()
        elapsed = end - start
        return {
            'url': url,
            'status': response.status,
            'time': elapsed,
        }
        

async def main():
    async with aiohttp.ClientSession() as session:
        tasks = [make_request(session, server_url) for _ in range(2)]
        responses = await asyncio.gather(*tasks)
        for response in responses:
            if response:
                print(response)


if __name__ == "__main__":
    asyncio.run(main())