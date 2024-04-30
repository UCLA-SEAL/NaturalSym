from faker import Faker
import random
from datetime import datetime, timedelta

def generate_data(num_records):
    fake = Faker()
    airports = ['LAX', 'JFK', 'ORD', 'ATL', 'DFW', 'DEN', 'SFO', 'SEA', 'MIA', 'LAS']
    with open("./input1.csv", 'w') as f:
        for i in range(num_records):
            passenger_id = f'P{i+1:03}'  # Generate PassengerID like P001, P002, P003, ...
            date = fake.date_between(start_date='-5y', end_date='today')  # Generate a date within the last 5 years
            
            # Generate random arrival and departure times within a reasonable range
            min_time = datetime.strptime('00:00', '%H:%M').time()
            max_time = datetime.strptime('23:59', '%H:%M').time()
            arrive_time = datetime.combine(datetime.today(), min_time) + timedelta(minutes=random.randint(0, 1439))
            departure_time = datetime.combine(datetime.today(), min_time) + timedelta(minutes=random.randint(0, 1439))
            
            # Ensure departure time is before arrival time
            while departure_time >= arrive_time:
                departure_time = datetime.combine(datetime.today(), min_time) + timedelta(minutes=random.randint(0, 1439))

            airport_id = random.choice(airports)  # Randomly select an airport from the list

            # Output the generated row
            row = f'{passenger_id},{date},{arrive_time.strftime("%H:%M")},{departure_time.strftime("%H:%M")},{airport_id}'
            print(row)
            f.write(row)
            f.write("\n")

if __name__ == '__main__':
    num_records = 1000  # Change this to the number of synthetic records you want to generate
    generate_data(num_records)