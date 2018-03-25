
	public void onPreviewFrame(byte[] data, Camera camera)
	{
		// The frame data will push to here.
		if (imageFormat == ImageFormat.NV21)
		{
			//Only accept the NV21(YUV420) format.
			//Post NV21 format data for further processing
			if ( !bProcessing )
			{
				FrameData = data;
				mHandler.post(DoImageProcessing);
			}
		}
	}