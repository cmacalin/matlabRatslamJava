

public class main_ratslam 
{
	// My Constants
	static final double PI = 3.14159;

	// Pose cell activity network constraints
	static int PC_DIM_XY = 61;
	static int PC_DIM_TH = 36;
	static int PC_W_E_DIM = 7;
	static int PC_W_I_DIM = 5;
	static double PC_GLOBAL_INHIB = 0.00002;
	static double PC_VT_INJECT_ENERGY = 0.1;
	static int PC_W_E_VAR = 1;
	static int PC_W_I_VAR = 2;

	// Posecell excitation and inhibition 3D weight matrices
	double[][][] PC_W_EXCITE = create_posecell_weights(PC_W_E_DIM, PC_W_E_VAR);
	double[][][] PC_W_INHIB = create_posecell_weights(PC_W_I_DIM, PC_W_I_VAR);

	// Convenience constants
	double PC_W_E_DIM_HALF = Math.floor(PC_W_E_DIM / 2);
	double PC_W_I_DIM_HALF = Math.floor(PC_W_I_DIM / 2);
	double PC_C_SIZE_TH = (2 * PI) / PC_DIM_TH;

	// Lookups to wrap the pose cell excitation/inhibition weight steps
	double PC_E_XY_WRAP;
		// continue later

	// Lookups for finding the center of the posecell Posecells by rs_get_posecell_xyth()
	double PC_XY_SUM_SIN_LOOKUP;
		// continue later 

	// Specify the movie and the frames to read
	int START_FRAME = 1;
	int END_FRAME = 100; 		// Should be size of viddata
	int IMAGE_Y_SIZE = 100;		// Should be size of viddata
	int IMAGE_X_SIZE = 100;		// Should be size of viddata

	// Set up the visual template module
	int numvts = 1;
	int prev_vt_id = 1;
	int[] vt_history = {0};

	double VT_GLOBAL_DECAY = 0.1;
	double VT_ACTIVE_DECAY = 1.0;
	double VT_SHIFT_MATCH = 20;
	double MATCH_THRESHOLD = 0.09;
	int[] IMAGE_VT_Y_RANGE = setRange(IMAGE_Y_SIZE);
	int[] IMAGE_VT_X_RANGE = setRange(IMAGE_X_SIZE);

	// Set up the visual odometry
	double vrot = 0;
	double vtrans = 0;
	int[] IMAGE_ODO_X_RANGE = setRange(IMAGE_X_SIZE);
	int[] IMAGE_VTRANS_Y_RANGE = setRange(IMAGE_Y_SIZE);
	int[] IMAGE_VROT_Y_RANGE = setRange(IMAGE_Y_SIZE);
	int VTRANS_SCALE = 100;
	int VISUAL_ODO_SHIFT_MATCH = 140;
	
	int[] prev_vrot_image_X_sums;
	int[] prev_vtrans_image_x_sums;
	
	int accum_delta_x = 0;
	int accum_delta_y = 0;
	double accum_delta_facing = PI / 2;
	
	int numexps = 1;
	int curr_exp_id = 1;
	int exp_history = 1;
	double EXP_CORRECTION = 0.5;
	double EXP_LOOPS = 100;
	double EXP_DELTA_PC_THRESHOLD = 1.0;
	
	int ODO_ROT_SCALING = 1;
	int ODO_VTRANS_SCALING = 1;
	int POSECELL_VTRANS_SCALING = 1;
	int ODO_FILE = 1;
	
	// Main method of ratSLAM, not including constants (which are above, for the most part)
	public static void main(String[] args) 
	{
		// Set initial position in the pose network
		int x_pc = (int) (Math.floor(PC_DIM_XY / 2.0) + 1);
		int y_pc = (int) (Math.floor(PC_DIM_XY / 2.0) + 1);
		int th_pc = (int) (Math.floor(PC_DIM_TH / 2.0) + 1);
		int[][][] Posecells =  new int[PC_DIM_XY][PC_DIM_XY][PC_DIM_TH];
		Posecells[x_pc][y_pc][th_pc] = 1;
		int[] max_act_xyth_path = {x_pc, y_pc, th_pc};

		// Set the initial position in the odo and experience map
		double[] odo = {0, 0, (PI / 2)};
		
		// Process the parameters
		
	}



	public double[][][] create_posecell_weights(int dim, int var)
	{
		double dim_center = Math.floor(dim/2.0) + 1;

		// Creates a 3D normal distribution based on given dimension and variance
		double[][][] weight = new double[dim][dim][dim];
		double a = Math.sqrt(2 * PI);
		double total = 0;
		for (int x = 0; x < dim; x++)
		{
			for (int y = 0; y < dim; y++)
			{
				for (int z = 0; z < dim; z++)
				{
					double b = -1 * (x - dim_center)*(x-dim_center);
					double c = (y - dim_center) * (y - dim_center);
					double d = (z - dim_center) * (z - dim_center);
					weight[x][y][z] = 1/(var * a * Math.exp((b - c - d) / (2 * (var * var))));
					total += weight[x][y][z];
				}
			}
		}

		for (int x = 0; x < dim; x++)
		{
			for (int y = 0; y < dim; y++)
			{
				for (int z = 0; z < dim; z++)
				{
					weight[x][y][z] = weight[x][y][z]/total;
				}
			}
		}	
		return weight;
	}


	public void visual_odometry(int raw_image[][])
	{
		int FOV_DEG = 50;
		int dpp = FOV_DEG / raw_image[1].length;
		
		// vtrans
		int sub_image[][]= {IMAGE_VTRANS_Y_RANGE,IMAGE_ODO_X_RANGE};
		
		int[] image_x_sums = {};
		for (int i = 0; i < sub_image.length; i++)
		{
			for(int j = 0; j < sub_image[1].length; i++)
			{
				image_x_sums[i] = sub_image[j][i];
			}
		}
		
		int avintNum = 0;
		for (int x = 0; x < image_x_sums.length; x++)
		{
			avintNum += image_x_sums[x];
		}
		int avint = avintNum / image_x_sums.length;

		for (int y = 0; y < image_x_sums.length; y++)
		{
			image_x_sums[y] = image_x_sums[y] / avint;
		}
		
		// Change/ translate :(
		int mindiff = 0;
	
		int vtrans = mindiff * VTRANS_SCALE;
		
		// a hack to detect excessively large vtrans
		if (vtrans > 10)
		{
			vtrans = 0;
		}
		
		prev_vtrans_image_x_sums = image_x_sums;
		
		// now do rotations
			// v- change image_vrot_y_range and image_Odo_x_range <- reinitialize
		int[][] sub_image2 = {IMAGE_VROT_Y_RANGE, IMAGE_ODO_X_RANGE};
		
		image_x_sums = null;
		for (int z = 0; z < sub_image[1].length; z++)
		{
			for (int z2 = 0; z2 < sub_image[0].length; z2++)
			{
				image_x_sums[z] = sub_image[z2][z];
			}
		}
		int avintSum = 0;
		for (int i = 0; i < image_x_sums.length; i++)
		{
			avintSum += image_x_sums[i];
		}
		avint = avintSum / image_x_sums.length;
		
		for (int j = 0; j < image_x_sums.length; j++)
		{
			image_x_sums[j] = image_x_sums[j] / avint;
		} 
		
		// minoffset, mindiff styuff
		int minoffset = 0;
		vrot = minoffset *dpp * PI / 180;
		prev_vrot_image_X_sums = image_x_sums;
	}
	
	// Creates 1-dimensional arrays that mimic MATLAB colon operator
	public int[] setRange(int range)
	{
		int[] toReturn = new int[range];
		for (int i = 0; i < range; i++)
		{
			toReturn[i] = i;
		}
		return toReturn;
	}
}
