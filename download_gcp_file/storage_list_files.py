#!/usr/bin/env python

# Copyright 2019 Google Inc. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the 'License');
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import sys
import os

# [START storage_list_files]
from google.cloud import storage


def list_blobs(bucket_name, prefix, delimiter=None):
    """Lists all the blobs in the bucket."""
    # bucket_name = "your-bucket-name"

    print("GOOGLE_APPLICATION_CREDENTIALS:", os.environ['GOOGLE_APPLICATION_CREDENTIALS'])

    storage_client = storage.Client()
    # storage_client = storage.Client.from_service_account_json( 'smart-grid-333211-2e2d841b2d83.json')


    # Note: Client.list_blobs requires at leastGOOGLE_APPLICATION_CREDENTIALS package version 1.17.0.
    blobs = storage_client.list_blobs(bucket_name, prefix=prefix, delimiter=delimiter)

    for blob in blobs:
        print(blob.name)


# [END storage_list_files]


if __name__ == "__main__":
    list_blobs(bucket_name=sys.argv[1], prefix=sys.argv[2])#, delimiter=sys.argv[3])

# https://github.com/googleapis/python-storage/blob/3664ddebe8746d0395acd86d5efa1ef973eeac3d/samples/snippets/storage_list_files.py
# pip install google-cloud-storage
# export GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account-key/smart-grid-333211-2e2d841b2d83.json

# SET GOOGLE_APPLICATION_CREDENTIALS=C:\Users\phili\git\stage\smartgrids\energy2\download_gcp_file\named-perigee-350713-099d04b75b0f.json
# SET GOOGLE_APPLICATION_CREDENTIALS=C:\Users\phili\git\stage\smartgrids\energy2\download_gcp_file\smart-grid-333211-2e2d841b2d83.json

# usage exemple :
# python storage_list_files.py data-grid SE05000283/2022-0









