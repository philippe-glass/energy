from mqtt_payload_decoder import PayloadDecoder
import sys
import pandas as pd
import matplotlib.pyplot as plt

def main(argv):
    try:
        print(sys.argv[1:])
        filenames = sys.argv[1:]
    except:
        print("usage: python3 mqtt_file_reader.py <filename of file with binary data>\n")
        exit()
    
    s_l1 = []
    index = []



    for filename in filenames:
        f = open(filename, 'rb')
        while True:
            binary = f.read(1)
            try:
                test = binary[0]
            except:
                print('end of file')
                break
            err, data = PayloadDecoder().decode_msg_type(binary)
            if not err:
                binary += f.read(data-1)
                #print(len(binary))
                err, data = PayloadDecoder().decode_feature(binary)
                print("data step2", data)
                if not err:
                    if data['feature_type'] == 17:   # one minute
                        s_l1.append(data['s_l1'])
                        index.append(data['timestamp'])

    idx = pd.to_datetime(index, unit="s")
    df1 = pd.DataFrame(index=idx)
    df1['s_l1'] = s_l1

    df1.plot()
    #print(df1)
    #print(df1.sort_index())

if __name__ == '__main__':

    main(sys.argv[1:])

    plt.show()

# usage exemple :
# python plot_power.py SE05000283/2022-04-30_09\:30.raw SE05000283/2022-04-30_10\:30.raw