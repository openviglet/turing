import { Component, Input } from '@angular/core';

@Component({
  selector: 'shio-logo',
  template: `
<svg class="mr-1" [style.width.px]="size" [style.height.px]="size" version="1.1" aria-hidden="true" viewBox="0, 0, 400,400"><g id="svgg"><path id="path0" d="M30.664 1.101 C 27.293 2.276,25.115 3.357,26.953 2.942 C 27.490 2.820,26.523 3.413,24.805 4.260 C 23.086 5.106,21.108 6.181,20.410 6.648 C 19.712 7.116,19.141 7.404,19.141 7.289 C 19.141 7.174,20.195 6.428,21.484 5.632 C 22.773 4.837,23.828 4.112,23.828 4.022 C 23.828 3.714,20.120 5.899,18.945 6.900 C 18.301 7.449,18.020 7.764,18.322 7.600 C 18.698 7.395,18.778 7.451,18.576 7.779 C 18.413 8.041,17.996 8.147,17.648 8.014 C 17.300 7.880,17.141 7.896,17.294 8.049 C 17.620 8.375,16.878 9.375,16.311 9.375 C 16.096 9.375,16.161 9.068,16.456 8.692 C 16.751 8.317,15.903 8.941,14.571 10.080 C 12.399 11.939,7.030 17.761,8.692 16.456 C 9.068 16.161,9.375 16.096,9.375 16.311 C 9.375 16.878,8.375 17.620,8.049 17.294 C 7.896 17.141,7.880 17.300,8.014 17.648 C 8.147 17.996,8.041 18.413,7.779 18.576 C 7.451 18.778,7.395 18.698,7.600 18.322 C 7.764 18.020,7.451 18.301,6.905 18.945 C 6.358 19.590,5.533 20.820,5.071 21.680 L 4.230 23.242 5.068 22.266 L 5.906 21.289 5.280 22.571 C 4.912 23.326,4.475 23.743,4.220 23.585 C 3.954 23.421,3.896 23.495,4.069 23.775 C 4.224 24.027,3.728 25.856,2.965 27.839 C -0.083 35.764,0.208 17.546,0.209 200.000 C 0.209 382.444,-0.082 364.239,2.965 372.161 C 3.728 374.144,4.224 375.973,4.069 376.225 C 3.896 376.505,3.954 376.579,4.220 376.415 C 4.475 376.257,4.912 376.674,5.280 377.429 L 5.906 378.711 5.068 377.734 L 4.230 376.758 5.071 378.320 C 5.533 379.180,6.358 380.410,6.905 381.055 C 7.451 381.699,7.764 381.980,7.600 381.678 C 7.395 381.302,7.451 381.222,7.779 381.424 C 8.041 381.587,8.147 382.004,8.014 382.352 C 7.880 382.700,7.896 382.859,8.049 382.706 C 8.375 382.380,9.375 383.122,9.375 383.689 C 9.375 383.904,9.068 383.839,8.692 383.544 C 7.098 382.293,12.307 387.969,14.600 389.982 C 17.531 392.555,18.277 392.899,15.903 390.582 L 14.258 388.977 16.211 390.465 C 18.432 392.158,19.226 392.853,18.397 392.380 C 18.017 392.163,17.939 392.214,18.143 392.544 C 18.306 392.807,18.679 392.930,18.972 392.817 C 19.266 392.704,19.627 392.808,19.774 393.046 C 19.953 393.334,19.858 393.369,19.494 393.149 C 19.046 392.879,19.033 392.928,19.427 393.414 C 19.923 394.027,23.828 396.286,23.828 395.960 C 23.828 395.860,23.037 395.314,22.070 394.747 C 21.104 394.181,20.313 393.622,20.312 393.506 C 20.312 393.390,20.891 393.653,21.599 394.091 C 22.307 394.528,24.021 395.421,25.408 396.075 C 26.795 396.729,27.490 397.170,26.953 397.055 L 25.977 396.846 26.999 397.432 C 28.534 398.311,33.799 400.006,34.834 399.955 C 35.623 399.915,35.639 399.886,34.961 399.735 C 33.405 399.387,28.079 397.572,28.215 397.436 C 28.291 397.360,29.753 397.765,31.462 398.337 L 34.570 399.378 58.398 399.643 C 89.348 399.988,310.652 399.988,341.602 399.643 L 365.430 399.378 368.538 398.337 C 370.247 397.765,371.709 397.360,371.785 397.436 C 371.921 397.572,366.595 399.387,365.039 399.735 C 364.361 399.886,364.377 399.915,365.166 399.955 C 366.201 400.006,371.466 398.311,373.001 397.432 L 374.023 396.846 373.047 397.055 C 372.510 397.170,373.205 396.729,374.592 396.075 C 375.979 395.421,377.693 394.528,378.401 394.091 C 379.109 393.653,379.688 393.390,379.688 393.506 C 379.687 393.622,378.896 394.181,377.930 394.747 C 376.963 395.314,376.172 395.860,376.172 395.960 C 376.172 396.286,380.077 394.027,380.573 393.414 C 380.967 392.928,380.954 392.879,380.506 393.149 C 380.142 393.369,380.047 393.334,380.226 393.046 C 380.373 392.808,380.734 392.704,381.028 392.817 C 381.321 392.930,381.694 392.807,381.857 392.544 C 382.061 392.214,381.983 392.163,381.603 392.380 C 380.774 392.853,381.568 392.158,383.789 390.465 L 385.742 388.977 384.097 390.582 C 381.723 392.899,382.469 392.555,385.400 389.982 C 387.693 387.969,392.902 382.293,391.308 383.544 C 390.932 383.839,390.625 383.904,390.625 383.689 C 390.625 383.122,391.625 382.380,391.951 382.706 C 392.104 382.859,392.120 382.700,391.986 382.352 C 391.853 382.004,391.959 381.587,392.221 381.424 C 392.549 381.222,392.605 381.302,392.400 381.678 C 392.236 381.980,392.549 381.699,393.095 381.055 C 393.642 380.410,394.467 379.180,394.929 378.320 L 395.770 376.758 394.935 377.734 C 394.248 378.538,394.301 378.295,395.234 376.367 C 396.793 373.145,398.285 369.079,399.104 365.820 C 400.172 361.570,400.172 38.427,399.104 34.180 C 398.285 30.920,396.792 26.853,395.234 23.633 C 394.301 21.705,394.248 21.462,394.935 22.266 L 395.770 23.242 394.929 21.680 C 394.467 20.820,393.642 19.590,393.095 18.945 C 392.549 18.301,392.236 18.020,392.400 18.322 C 392.605 18.698,392.549 18.778,392.221 18.576 C 391.959 18.413,391.853 17.996,391.986 17.648 C 392.120 17.300,392.104 17.141,391.951 17.294 C 391.625 17.620,390.625 16.878,390.625 16.311 C 390.625 16.096,390.932 16.161,391.308 16.456 C 392.902 17.707,387.693 12.031,385.400 10.018 C 382.469 7.445,381.723 7.101,384.097 9.418 L 385.742 11.023 383.789 9.535 C 381.568 7.842,380.774 7.147,381.603 7.620 C 381.983 7.837,382.061 7.786,381.857 7.456 C 381.694 7.193,381.321 7.070,381.028 7.183 C 380.734 7.296,380.373 7.192,380.226 6.954 C 380.047 6.666,380.142 6.631,380.506 6.851 C 380.954 7.121,380.967 7.072,380.573 6.586 C 380.077 5.973,376.172 3.714,376.172 4.040 C 376.172 4.140,376.963 4.686,377.930 5.253 C 378.896 5.819,379.687 6.378,379.687 6.494 C 379.687 6.610,379.109 6.347,378.401 5.909 C 377.693 5.472,375.979 4.579,374.592 3.925 C 373.205 3.271,372.510 2.830,373.047 2.945 L 374.023 3.154 372.992 2.564 C 371.535 1.729,366.078 -0.009,365.087 0.045 C 364.302 0.088,364.300 0.100,365.039 0.265 C 366.595 0.613,371.921 2.428,371.785 2.564 C 371.709 2.640,370.247 2.242,368.538 1.679 C 365.456 0.664,365.285 0.652,348.242 0.306 C 315.268 -0.364,36.504 0.186,33.868 0.927 C 30.883 1.764,30.493 1.670,33.203 0.765 C 34.385 0.371,35.000 0.039,34.570 0.029 C 34.141 0.019,32.383 0.501,30.664 1.101 M30.469 1.918 C 30.146 2.100,29.443 2.357,28.906 2.488 C 28.092 2.687,28.125 2.633,29.102 2.162 C 30.353 1.559,31.449 1.363,30.469 1.918 M11.870 13.477 C 10.636 14.873,9.540 16.016,9.433 16.016 C 9.228 16.016,10.237 14.816,12.473 12.402 C 14.705 9.994,14.283 10.745,11.870 13.477 M389.142 14.160 C 391.499 16.972,390.690 16.379,388.075 13.379 C 386.811 11.929,386.184 11.094,386.682 11.523 C 387.180 11.953,388.287 13.140,389.142 14.160 M362.563 12.269 C 375.338 14.722,385.288 24.658,387.725 37.396 C 388.556 41.741,388.562 358.235,387.731 362.563 C 385.278 375.338,375.342 385.288,362.604 387.725 C 358.233 388.561,41.767 388.561,37.396 387.725 C 24.658 385.288,14.722 375.338,12.269 362.563 C 11.438 358.235,11.444 41.741,12.275 37.396 C 14.682 24.814,24.565 14.828,37.109 12.303 C 41.043 11.512,358.445 11.478,362.563 12.269 M1.612 29.228 C 0.953 30.939,0.316 33.104,0.197 34.040 C 0.017 35.456,0.105 35.315,0.717 33.203 C 1.122 31.807,1.826 29.708,2.280 28.539 C 2.735 27.370,3.040 26.347,2.959 26.266 C 2.878 26.185,2.272 27.518,1.612 29.228 M396.891 26.172 C 396.882 26.279,397.251 27.334,397.711 28.516 C 398.170 29.697,398.863 31.807,399.250 33.203 C 399.636 34.600,399.964 35.334,399.976 34.834 C 400.013 33.443,396.987 24.950,396.891 26.172 M320.721 45.852 C 320.518 46.386,319.655 48.460,318.804 50.462 C 317.953 52.464,316.914 54.980,316.495 56.055 C 316.077 57.129,314.761 60.381,313.572 63.281 C 305.352 83.330,306.063 80.859,308.517 80.859 L 310.393 80.859 312.060 76.556 L 313.726 72.252 323.089 72.356 L 332.452 72.461 334.097 76.563 L 335.742 80.664 337.759 80.781 L 339.776 80.898 336.848 73.750 C 331.519 60.741,330.106 57.284,327.616 51.172 L 325.149 45.117 323.119 44.999 C 321.267 44.891,321.058 44.965,320.721 45.852 M344.792 45.182 C 344.648 45.326,344.531 53.411,344.531 63.151 L 344.531 80.859 346.289 80.859 L 348.047 80.859 348.047 62.891 L 348.047 44.922 346.549 44.922 C 345.726 44.922,344.935 45.039,344.792 45.182 M323.302 69.051 C 319.017 69.109,315.420 69.066,315.308 68.954 C 315.137 68.782,319.853 56.611,322.349 50.784 L 323.100 49.030 327.097 58.987 L 331.093 68.945 323.302 69.051 M51.262 176.270 L 51.367 184.570 73.730 184.671 L 96.094 184.772 96.094 248.050 L 96.094 311.328 105.078 311.328 L 114.063 311.328 114.063 248.047 L 114.063 184.766 136.523 184.766 L 158.984 184.766 158.984 176.367 L 158.984 167.969 105.070 167.969 L 51.156 167.969 51.262 176.270 M158.988 242.473 C 158.992 287.435,159.225 290.693,162.986 298.391 C 170.981 314.757,195.937 318.828,216.886 307.184 C 218.295 306.401,220.961 304.642,222.810 303.274 L 226.172 300.788 226.172 306.066 L 226.172 311.344 234.473 311.238 L 242.773 311.133 242.773 258.594 L 242.773 206.055 234.473 205.949 L 226.172 205.844 226.172 244.826 L 226.172 283.809 223.340 286.568 C 207.137 302.353,182.055 302.224,176.708 286.328 C 176.030 284.312,175.973 281.782,175.786 245.117 L 175.586 206.055 167.285 205.949 L 158.984 205.844 158.988 242.473 M0.197 365.960 C 0.411 367.646,2.675 374.018,2.959 373.734 C 3.040 373.653,2.735 372.630,2.280 371.461 C 1.826 370.292,1.122 368.193,0.717 366.797 C 0.105 364.685,0.017 364.544,0.197 365.960 M399.663 365.106 C 399.633 365.573,399.103 367.419,398.483 369.208 C 397.864 370.997,397.237 372.813,397.091 373.242 C 396.941 373.684,397.084 373.579,397.422 373.001 C 398.403 371.317,400.057 366.003,399.880 365.099 L 399.716 364.258 399.663 365.106 M11.925 386.621 C 13.189 388.071,13.816 388.906,13.318 388.477 C 12.321 387.616,9.206 383.984,9.465 383.984 C 9.554 383.984,10.661 385.171,11.925 386.621 M389.142 385.840 C 388.287 386.860,387.180 388.047,386.682 388.477 C 386.184 388.906,386.811 388.071,388.075 386.621 C 390.690 383.621,391.499 383.028,389.142 385.840 " stroke="none" fill="#fbdcac" fill-rule="evenodd"></path><path id="path1" d="M35.547 12.915 C 24.384 15.821,15.737 24.521,12.895 35.707 C 11.656 40.582,11.656 359.418,12.895 364.293 C 15.757 375.555,24.445 384.243,35.707 387.105 C 40.582 388.344,359.418 388.344,364.293 387.105 C 375.555 384.243,384.243 375.555,387.105 364.293 C 388.344 359.418,388.344 40.582,387.105 35.707 C 384.243 24.445,375.555 15.757,364.293 12.895 C 359.496 11.675,40.232 11.695,35.547 12.915 M325.650 45.605 C 325.924 46.403,329.367 54.865,332.048 61.328 C 340.869 82.599,340.470 81.250,337.938 81.250 C 335.672 81.250,335.726 81.313,333.807 76.465 L 332.299 72.656 323.083 72.661 L 313.867 72.665 312.148 76.958 L 310.428 81.250 308.339 81.250 C 305.769 81.250,305.763 81.566,308.452 75.057 C 311.875 66.771,314.153 61.218,316.569 55.273 C 317.791 52.266,319.241 48.706,319.791 47.363 L 320.791 44.922 323.103 44.922 C 324.955 44.922,325.462 45.058,325.650 45.605 M348.047 63.086 L 348.047 81.250 346.331 81.250 C 344.013 81.250,344.141 82.333,344.141 62.728 C 344.141 43.721,343.991 44.922,346.354 44.922 L 348.047 44.922 348.047 63.086 M322.259 51.429 C 321.994 52.254,321.018 54.775,320.090 57.031 C 319.162 59.287,317.765 62.715,316.986 64.648 C 316.207 66.582,315.475 68.384,315.360 68.652 C 315.197 69.034,316.870 69.141,323.005 69.141 C 327.325 69.141,330.859 69.039,330.859 68.914 C 330.859 68.789,330.002 66.636,328.955 64.129 C 327.907 61.622,326.220 57.549,325.206 55.078 C 322.985 49.666,322.870 49.524,322.259 51.429 M158.984 176.172 L 158.984 184.766 136.719 184.766 L 114.453 184.766 114.453 248.242 L 114.453 311.719 105.273 311.719 L 96.094 311.719 96.094 248.242 L 96.094 184.766 73.633 184.766 L 51.172 184.766 51.172 176.172 L 51.172 167.578 105.078 167.578 L 158.984 167.578 158.984 176.172 M175.881 244.434 C 176.022 287.048,175.887 284.579,178.343 289.592 C 184.538 302.235,209.477 300.224,223.729 285.931 L 225.778 283.877 225.877 244.770 L 225.977 205.664 234.473 205.559 L 242.969 205.454 242.969 258.586 L 242.969 311.719 234.386 311.719 L 225.804 311.719 225.695 306.639 L 225.586 301.558 222.461 303.899 C 202.073 319.166,172.208 316.906,163.195 299.414 C 158.968 291.210,158.989 291.486,158.987 244.824 L 158.984 205.469 167.369 205.469 L 175.753 205.469 175.881 244.434 " stroke="none" fill="#436be4" fill-rule="evenodd"></path><path id="path2" d="M318.652 72.557 C 321.069 72.626,325.024 72.626,327.441 72.557 C 329.858 72.489,327.881 72.433,323.047 72.433 C 318.213 72.433,316.235 72.489,318.652 72.557 M307.525 81.140 C 308.119 81.229,308.998 81.227,309.478 81.134 C 309.958 81.042,309.473 80.968,308.398 80.971 C 307.324 80.974,306.931 81.050,307.525 81.140 M336.822 81.140 C 337.416 81.229,338.294 81.227,338.775 81.134 C 339.255 81.042,338.770 80.968,337.695 80.971 C 336.621 80.974,336.228 81.050,336.822 81.140 M345.610 81.136 C 346.096 81.229,346.799 81.226,347.173 81.128 C 347.546 81.030,347.148 80.954,346.289 80.958 C 345.430 80.962,345.124 81.042,345.610 81.136 M114.159 248.137 L 114.063 311.314 104.980 311.426 L 95.898 311.539 104.993 311.629 C 111.976 311.698,114.130 311.605,114.273 311.230 C 114.375 310.962,114.413 282.441,114.357 247.852 L 114.255 184.961 114.159 248.137 M225.969 244.727 C 225.969 266.211,226.016 275.057,226.074 264.385 C 226.132 253.713,226.132 236.135,226.074 225.323 C 226.016 214.510,225.969 223.242,225.969 244.727 M225.959 306.347 L 225.977 311.523 234.375 311.535 L 242.773 311.546 234.484 311.430 L 226.194 311.313 226.068 306.242 L 225.941 301.171 225.959 306.347 " stroke="none" fill="#7d89d5" fill-rule="evenodd"></path><path id="path3" d="M120.801 12.012 C 164.360 12.066,235.640 12.066,279.199 12.012 C 322.759 11.957,287.119 11.912,200.000 11.912 C 112.881 11.912,77.241 11.957,120.801 12.012 M11.912 200.000 C 11.912 287.119,11.957 322.759,12.012 279.199 C 12.066 235.640,12.066 164.360,12.012 120.801 C 11.957 77.241,11.912 112.881,11.912 200.000 M388.084 200.000 C 388.084 287.119,388.129 322.759,388.184 279.199 C 388.238 235.640,388.238 164.360,388.184 120.801 C 388.129 77.241,388.084 112.881,388.084 200.000 M344.321 62.891 C 344.321 72.666,344.371 76.665,344.433 71.777 C 344.495 66.890,344.495 58.892,344.433 54.004 C 344.371 49.116,344.321 53.115,344.321 62.891 M78.027 167.871 C 92.905 167.928,117.251 167.928,132.129 167.871 C 147.007 167.814,134.834 167.768,105.078 167.768 C 75.322 167.768,63.149 167.814,78.027 167.871 M162.967 205.760 C 165.264 205.829,169.131 205.829,171.560 205.761 C 173.989 205.692,172.109 205.636,167.383 205.635 C 162.656 205.635,160.669 205.691,162.967 205.760 M230.154 205.760 C 232.452 205.829,236.319 205.829,238.748 205.761 C 241.177 205.692,239.297 205.636,234.570 205.635 C 229.844 205.635,227.856 205.691,230.154 205.760 M196.973 313.941 C 197.349 314.039,197.964 314.039,198.340 313.941 C 198.716 313.843,198.408 313.763,197.656 313.763 C 196.904 313.763,196.597 313.843,196.973 313.941 M120.801 388.184 C 164.360 388.238,235.640 388.238,279.199 388.184 C 322.759 388.129,287.119 388.084,200.000 388.084 C 112.881 388.084,77.241 388.129,120.801 388.184 " stroke="none" fill="#6b7cdb" fill-rule="evenodd"></path><path id="path4" d="M159.091 282.422 C 159.091 283.389,159.167 283.784,159.260 283.301 C 159.353 282.817,159.353 282.026,159.260 281.543 C 159.167 281.060,159.091 281.455,159.091 282.422 M178.906 291.528 C 178.906 291.596,179.478 292.167,180.176 292.798 L 181.445 293.945 180.298 292.676 C 179.229 291.492,178.906 291.227,178.906 291.528 M166.406 304.028 C 166.406 304.096,166.978 304.667,167.676 305.298 L 168.945 306.445 167.798 305.176 C 166.729 303.992,166.406 303.727,166.406 304.028 M191.300 313.954 C 191.993 314.040,193.224 314.042,194.034 313.957 C 194.845 313.872,194.277 313.802,192.773 313.800 C 191.270 313.798,190.606 313.868,191.300 313.954 " stroke="none" fill="#949ccd" fill-rule="evenodd"></path></g></svg>`,
})
export class ShioLogoComponent {
  @Input() size: number;
}
