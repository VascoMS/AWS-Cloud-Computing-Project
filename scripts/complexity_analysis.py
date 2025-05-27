import requests
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os

webserver_ip = "localhost"  # Change to your server's IP if needed
server_url = f"http://{webserver_ip}:8000"

def send_request(game, params):
    """
    Sends a GET request to the server and prints the response.
    """
    try:
        response = requests.get(f"{server_url}/{game}", params=params)
        elapsed_time = response.elapsed.total_seconds()
        if response.status_code != 200:
            print(f"Error: {response.status_code} - {response.text}")
            return None, elapsed_time
        requestStatistics = response.json().get("requestStatistics")
        print(f"Params: {params}, requestStatistics: {requestStatistics}, Elapsed Time: {elapsed_time:.4f} seconds")
        return requestStatistics, elapsed_time
    except requests.exceptions.RequestException as e:
        print("Error sending request:", e)


def analyze_complexity(game, params_list):
    results = []
    for params in params_list:
        requestStatistics, elapsed_time = send_request(game, params)
        if requestStatistics is not None:
            results.append({
                "complexity": requestStatistics.get("complexity", 0),
                "nblocks": requestStatistics.get("nblocks", 0),
                "nmethod": requestStatistics.get("nmethod", 0),
                "ninsts": requestStatistics.get("ninsts", 0),
                "ndataWrites": requestStatistics.get("ndataWrites", 0),
                "ndataReads": requestStatistics.get("ndataReads", 0),
                "elapsed_time": elapsed_time
            })
        else:
            print(f"Failed to get complexity for params: {params}")
    return pd.DataFrame(results)

def plot_results(df, game_name):
    """
    Creates and saves a separate plot for each metric vs elapsed time.
    Saves plots to the 'charts' directory.
    """
    metrics = ['complexity', 'nblocks', 'nmethod', 'ninsts', 'ndataWrites', 'ndataReads']
    df_sorted = df.sort_values(by='elapsed_time')

    # Create charts directory if it doesn't exist
    os.makedirs("charts", exist_ok=True)

    for metric in metrics:
        plt.figure(figsize=(10, 6))
        plt.plot(df_sorted['elapsed_time'], df_sorted[metric], marker='o', alpha=0.7, label=metric)
        plt.xlabel('Elapsed Time (s)')
        plt.ylabel(metric)
        plt.title(f'{metric} vs Elapsed Time - {game_name}')
        plt.grid(True, alpha=0.3)
        plt.legend()
        plt.tight_layout()

        # Save the figure with a descriptive name
        filename = f"charts/{game_name}_{metric}_vs_elapsed_time.png"
        plt.savefig(filename)
        plt.close()


def main():
    capturetheflag_params = [
        {"gridSize": 30, "numBlueAgents": blue, "numRedAgents": red, "flagPlacementType": "A"}
        for blue, red in zip(range(5, 30), range(5, 30))
    ]
    fifteenpuzzle_params = [
        {"size": 20, "shuffles": shuffles}
        for shuffles in range(70, 86, 2)
    ]
    gameoflife_params = [
        {"mapFilename": "glider-10-10.json", "iterations": iterations}
        for iterations in range(1000, 20000, 1000)
    ]
    params_list = {
        #"capturetheflag": capturetheflag_params,
        "fifteenpuzzle": fifteenpuzzle_params,
        #"gameoflife": gameoflife_params
    }

    for game, params in params_list.items():
        df = analyze_complexity(game, params)
        plot_results(df, game)

if __name__ == "__main__":
    main()