package org.jlab.service.dc;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jlab.clas.reco.ReconstructionEngine;
import org.jlab.detector.banks.RawBank.OrderType;
import org.jlab.io.base.DataBank;
import org.jlab.io.base.DataEvent;
import org.jlab.rec.dc.Constants;
import org.jlab.rec.dc.banks.Banks;
import org.jlab.clas.tracking.kalmanfilter.zReference.KFitter;
import org.jlab.clas.tracking.kalmanfilter.zReference.DAFilter;

public class DCEngine extends ReconstructionEngine {

    // bank names
    private final Banks  bankNames = new Banks();
    
    // options configured from yaml
    private int        selectedSector = 0;
    private String     ministaggerStatus = null;
    private String     feedthroughsStatus = null;
    private boolean    wireDistortion = false;
    private boolean    useStartTime   = true;
    private boolean    useBetaCut     = false;
    private boolean    useDoublets    = true;
    private boolean    dcrbJitter     = false;
    private boolean    swapDCRBBits   = false;
    private int        t2d            = 1;
    private int        nSuperLayer    = 5;
    private String     geoVariation   = "default";
    private String     bankType       = "HitBasedTrkg";
    private String     inBankPrefix   = null;
    private String     outBankPrefix  = null;
    private double[][] shifts         = new double[Constants.NREG][6];
    protected boolean  useDAF         = true;
    private String   dafChi2Cut     = null;
    private String   dafAnnealingFactorsTB = null;
    
    public static final Logger LOGGER = Logger.getLogger(ReconstructionEngine.class.getName());


    public DCEngine(String name) {
        super(name,"ziegler","5.0");
    }

    public void setOptions() {
        
        // set the geometry variation, if dcGeometryVariation is undefined, default to variation
        if(this.getEngineConfigString("dcGeometryVariation")!=null) 
            geoVariation = this.getEngineConfigString("dcGeometryVariation");
        else if(this.getEngineConfigString("variation")!=null) 
            geoVariation = this.getEngineConfigString("variation");
        
        //AI settings for selecting specific sector
        if(this.getEngineConfigString("sectorSelect")!=null) 
            selectedSector=Integer.parseInt(this.getEngineConfigString("sectorSelect"));
            
        // Load config
        if(this.getEngineConfigString("dcUseStartTime")!=null)
            useStartTime = Boolean.valueOf(this.getEngineConfigString("dcUseStartTime"));
      
        // R3 ministagger
        if(this.getEngineConfigString("dcMinistagger")!=null)       
            ministaggerStatus = this.getEngineConfigString("dcMinistagger");
        
        // Wire feedthroughs
        if(this.getEngineConfigString("dcFeedthroughs")!=null)       
            feedthroughsStatus = this.getEngineConfigString("dcFeedthroughs");
        
        // Wire distortions
        if(this.getEngineConfigString("dcWireDistortion")!=null)       
            wireDistortion = Boolean.parseBoolean(this.getEngineConfigString("dcWireDistortion"));
        
        //Use beta cut(true: use time; false: use track doca)
        if(this.getEngineConfigString("dcBetaCut")!=null)
            useBetaCut =Boolean.valueOf(this.getEngineConfigString("dcBetaCut"));
        
        //T2D Function
        if(this.getEngineConfigString("dcT2DFunc")!=null) {      
            if(this.getEngineConfigString("dcT2DFunc").equalsIgnoreCase("Exponential"))
                t2d=0;
            else if(this.getEngineConfigString("dcT2DFunc").equalsIgnoreCase("Polynomial"))
                t2d=1;
        }
        
        //Recover hit doublets
        if(this.getEngineConfigString("dcDoublets")!=null)       
            useDoublets = Boolean.valueOf(this.getEngineConfigString("dcDoublets"));
        
        //Apply the jitter correction based on DCRB timestamps
        if(this.getEngineConfigString("dcrbJitter")!=null)       
            dcrbJitter = Boolean.valueOf(this.getEngineConfigString("dcrbJitter"));
                
        //Swap DCRB timestamp bits
        if(this.getEngineConfigString("swapDCRBBits")!=null)       
            swapDCRBBits = Boolean.valueOf(this.getEngineConfigString("swapDCRBBits"));
        
        //NSUPERLAYERTRACKING
        if(this.getEngineConfigString("dcFOOST")!=null)
            if(!Boolean.valueOf(this.getEngineConfigString("dcFOOST"))) {
                nSuperLayer =6;
            }    
                        
        //Set input bank names
        if(this.getEngineConfigString("inputBankPrefix")!=null) {
            inBankPrefix = this.getEngineConfigString("inputBankPrefix");
        }

        //Set output bank names
        if(this.getEngineConfigString("outputBankPrefix")!=null) {
            outBankPrefix = this.getEngineConfigString("outputBankPrefix");
        }
        
        //Set if use DAF
        if(this.getEngineConfigString("useDAF")!=null) 
            useDAF=Boolean.valueOf(this.getEngineConfigString("useDAF"));
        
        if(this.getEngineConfigString("dafChi2Cut")!=null) {
            dafChi2Cut=this.getEngineConfigString("dafChi2Cut");
            DAFilter.setDafChi2Cut(Double.valueOf(dafChi2Cut));
        }
        
        if(this.getEngineConfigString("dafAnnealingFactorsTB")!=null){ 
            dafAnnealingFactorsTB=this.getEngineConfigString("dafAnnealingFactorsTB");
            KFitter.setDafAnnealingFactorsTB(dafAnnealingFactorsTB);
        }
               
        // Set geometry shifts for alignment code
        if(this.getEngineConfigString("alignmentShifts")!=null) {
            String[] alignmentShift = this.getEngineConfigString("alignmentShifts").split(",");
            System.out.println(alignmentShift[0]);
            for(int i=0; i<alignmentShift.length; i++) {
                if(alignmentShift[i].strip().matches("r[123]_c?[xyz]:.*")) {
                    String shift = alignmentShift[i].split(":")[0];
                    double value = Double.parseDouble(alignmentShift[i].split(":")[1]);
                    int region = Integer.parseInt(shift.substring(1, 2));
                    int iaxis = 0;
                    if(shift.endsWith("y"))      iaxis = 1;
                    else if(shift.endsWith("z")) iaxis = 2;
                    if(shift.contains("c")) iaxis +=3;
                    shifts[region-1][iaxis] = value;
                }
            }
        }        
    }


    public void LoadTables() {

        // Load tables
        Map<String,Integer> dcTables = new HashMap<>();
        dcTables.put(Constants.TT,3);
        dcTables.put(Constants.DOCARES,3);
        dcTables.put(Constants.TIME2DIST,3);
        dcTables.put(Constants.PRESSURE, 3);
        dcTables.put(Constants.T2DPRESSURE,3);
        dcTables.put(Constants.T2DPRESSUREREF,3);
        dcTables.put(Constants.T0CORRECTION,4);
        dcTables.put(Constants.TDCTCUTS,3);
        dcTables.put(Constants.TIMEJITTER,3);
        dcTables.put(Constants.WIRESTAT,3);
        dcTables.put(Constants.BEAMPOS,3);
        
        requireConstants(dcTables);
        this.getConstantsManager().setVariation("default");
    }
        
    
    @Override
    public boolean processDataEvent(DataEvent event) {
        return true;
    }

    @Override
    public boolean init() {
        this.setOptions();
        Constants.getInstance().initialize(this.getName(),
                                           geoVariation, 
                                           ministaggerStatus, 
                                           feedthroughsStatus,
                                           wireDistortion, 
                                           useStartTime, 
                                           useBetaCut, 
                                           t2d,
                                           useDoublets,
                                           dcrbJitter,
                                           swapDCRBBits,
                                           nSuperLayer, 
                                           selectedSector,
                                           shifts);
        this.LoadTables();
        this.initBanks();
        this.setDropBanks();
        return true;
    }

    private void initBanks() {
        if(inBankPrefix==null && outBankPrefix!=null) 
            this.getBanks().init(outBankPrefix);
        if(inBankPrefix!=null && outBankPrefix!=null) 
            this.getBanks().init(inBankPrefix, outBankPrefix);
        LOGGER.log(Level.INFO,"["+this.getName()+"] bank names set for " + this.getBanks().toString());       
    }

    public Banks getBanks() {
        return bankNames;
    }
        
    public void setDropBanks() {
        
    }
    
    public OrderType[] getRawBankOrders() {
        return this.rawBankOrders;
    }
    
    public int getRun(DataEvent event) {
        if (!event.hasBank("RUN::config")) {
            return 0;
        }
        DataBank bank = event.getBank("RUN::config");
        LOGGER.log(Level.FINE,"["+this.getName()+"] EVENT "+bank.getInt("event", 0));       
        
        int run = bank.getInt("run", 0);
        return run;
    }
}
