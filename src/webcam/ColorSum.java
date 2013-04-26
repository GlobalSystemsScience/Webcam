package webcam;

import javax.media.Buffer;
import javax.media.Codec;
import javax.media.Format;
import javax.media.ResourceUnavailableException;
import javax.media.format.VideoFormat;

public class ColorSum implements Codec {
	private Grapher grapher;

	static Format[] supportedFormats = new Format[] { new VideoFormat(
			VideoFormat.RGB) };

	public ColorSum() {
	}

	public void setGrapher(Grapher g) {
		grapher = g;
	}

	@Override
	public Format[] getSupportedInputFormats() {
		return supportedFormats;
	}

	@Override
	public Format[] getSupportedOutputFormats(Format input) {
		if (input == null)
			return supportedFormats;
		if (supportedFormats[0].matches(input))
			return new Format[] { input };
		else
			return new Format[] {};
	}

	@Override
	public int process(Buffer input, Buffer output) {
		output.copy(input);
		byte[] data = (byte[]) input.getData();
		if (data != null)
			grapher.graph(data);
		return BUFFER_PROCESSED_OK;
	}

	@Override
	public Format setInputFormat(Format format) {
		if (supportedFormats[0].matches(format)) {
			return format;
		} else
			return null;
	}

	@Override
	public Format setOutputFormat(Format format) {
		if (supportedFormats[0].matches(format)) {
			return format;
		} else
			return null;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		return getClass().getName();
	}

	@Override
	public void open() throws ResourceUnavailableException {
	}

	@Override
	public void reset() {
	}

	@Override
	public Object getControl(String controlType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getControls() {
		return new Object[0];
	}

}
