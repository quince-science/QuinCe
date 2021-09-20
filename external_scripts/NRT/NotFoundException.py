class NotFoundException(Exception):
    def __init__(self, item_type, item_name):
        super.__init__(f'{item_type} \'{item_name}\' not found')