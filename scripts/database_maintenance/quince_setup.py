import re

class QuinceSetup:
    def __init__(self, setup_path):
        with open(setup_path, 'r') as r:
            self._props = r.read()


    def get_prop(self, name):
        match = re.search(f'%{name}%=(.*)', self._props)
        return match.group(1)

    def get_db_info(self):
        return (self.get_prop('db_host'), self.get_prop('db_port'),
            self.get_prop('db_database'), self.get_prop('db_username'),
            self.get_prop('db_password'))