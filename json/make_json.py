import random
import string
import json
import sys

# make a random json file of approximately a specified size in MB
# the json file is a list of trees. each node in the tree is a 
# "person" with a random name, birthday, id number, height and weight.
# they also have a chance to have a child or two, which creates a nested
# json object.

class RandomJson:

    def __init__(self, count):
        self.count = count
        self.branch_list = []
        while self.count > 0:
            self.branch_list.append(self.get_branch())

    def random_name(self, min_length, max_length):
        length = random.randint(min_length, max_length)
        return ''.join(random.choices(string.ascii_lowercase, k=length)).title()

    def random_date(self):
        return str(random.randint(1920, 2024)) + "-" \
             + str(random.randint(1, 12)) + "-" \
             + str(random.randint(1, 31))
    
    def random_height(self):
        return str(random.randint(5, 6)) + "'" \
             + str(random.randint(0, 11))

    def get_branch(self):
        self.count -= 1
        d = {"name": self.random_name(3, 10) + " " + self.random_name(3, 10),
             "birthday": self.random_date(),
             "id": random.randint(1111111, 9999999),
             "height": self.random_height(),
             "weight": random.randint(100, 200)
            }
        
        if self.count > 0 and random.random() > 0.5:
            d["child1"] = self.get_branch()
            if self.count > 0 and random.random() > 0.5:
                d["child2"] = self.get_branch()

        return d


if __name__ == "__main__":

    # argv[1] is the rough size in MB
    random_json = RandomJson(4150*int(sys.argv[1]))
    with open(f"{sys.argv[2]}.json", "w") as f:
        json.dump(random_json.branch_list, f, indent=4)
