import requests
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt


server_url = "http://34.207.141.3:8000"

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
        complexity = response.json().get("complexityScore")
        print(f"Params: {params}, Complexity: {complexity}, Elapsed Time: {elapsed_time:.4f} seconds")
        return complexity, elapsed_time
    except requests.exceptions.RequestException as e:
        print("Error sending request:", e)


def analyze_complexity(game, params_list):
    results = []
    for params in params_list:
        complexity, elapsed_time = send_request(game, params)
        if complexity is not None:
            results.append({
                "complexity": complexity,
                "elapsed_time": elapsed_time
            })
        else:
            print(f"Failed to get complexity for params: {params}")
    return pd.DataFrame(results)

def plot_results(df):
    """
    Plots the complexity vs elapsed time using matplotlib.
    """
    
    plt.figure(figsize=(10, 6))
    plt.scatter(df['complexity'], df['elapsed_time'], alpha=0.7)
    plt.xlabel('Complexity Score')
    plt.ylabel('Response Time (s)')
    plt.title('Response Time vs Complexity')
    
    # Add a best fit line to visualize correlation
    if len(df) > 1:
        z = np.polyfit(df['complexity'], df['elapsed_time'], 1)
        p = np.poly1d(z)
        plt.plot(df['complexity'], p(df['complexity']), "r--", alpha=0.8, 
                 label=f"Trend Line (y={z[0]:.4f}x+{z[1]:.4f})")
        plt.legend()
    
    plt.grid(True, alpha=0.3)
    plt.show()


def main():
    game = "capturetheflag"
    gridSize = 30
    params_list = [
        {"gridSize": gridSize, "numBlueAgents": blue, "numRedAgents": red, "flagPlacementType": "A"}
        for blue, red in zip(range(5, 30), range(5, 30))
    ]
    df = analyze_complexity(game, params_list)
    plot_results(df)

if __name__ == "__main__":
    main()
