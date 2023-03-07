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

# [START storage_download_file]
from google.cloud import storage


def download_blob(bucket_name, source_blob_name, directory_dest):
    """Downloads a blob from the bucket."""
    # The ID of your GCS bucket
    # bucket_name = "your-bucket-name"

    # The ID of your GCS object
    # source_blob_name = "storage-object-name"

    # The path to which the file should be downloaded
    # destination_file_name = "local/path/to/file"
    directory_dest = directory_dest + source_blob_name
    os.makedirs(os.path.dirname(directory_dest), exist_ok=True)

    storage_client = storage.Client()

    bucket = storage_client.bucket(bucket_name)

    # Construct a client side representation of a blob.
    # Note `Bucket.blob` differs from `Bucket.get_blob` as it doesn't retrieve
    # any content from Google Cloud Storage. As we don't need additional data,
    # using `Bucket.blob` is preferred here.
    blob = bucket.blob(source_blob_name)
    blob.download_to_filename(directory_dest)

    print(
        "Downloaded storage object {} from bucket {} to local file {}.".format(
            source_blob_name, bucket_name, directory_dest
        )
    )


# [END storage_download_file]

if __name__ == "__main__":
    try:
        directory = sys.argv[3]
    except:
        directory = './'
    download_blob(
        bucket_name=sys.argv[1],
        source_blob_name=sys.argv[2],
        directory_dest=directory,
    )

# https://github.com/googleapis/python-storage/blob/3664ddebe8746d0395acd86d5efa1ef973eeac3d/samples/snippets/storage_download_file.py
# usage exemple :
# python storage_download_file.py data-grid SE05000283/2022-04-30_09:30.raw