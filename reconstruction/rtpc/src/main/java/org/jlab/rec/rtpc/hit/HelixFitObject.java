/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.rec.rtpc.hit;

/**
 *
 * @author davidpayette
 */
public class HelixFitObject {
    private double _Rho;
    private double _A;
    private double _B;
    private double _Phi;
    private double _Theta;
    private double _X0;
    private double _Y0;
    private double _Z0;
    private double _DCA;
    private double _Chi2;
    
    public HelixFitObject(){
        //default constructor
    }
    
    public HelixFitObject(double Rho, double A, double B, double Phi, double Theta, double X0, double Y0, double Z0, double DCA, double Chi2){
        _Rho = Rho;
        _A = A;
        _B = B;
        _Phi = Phi;
        _Theta = Theta;
        _X0 = X0;
        _Y0 = Y0;
        _Z0 = Z0;
        _DCA = DCA;
        _Chi2 = Chi2;
    }
    public double get_Rho(){
        return _Rho;
    }
    public void set_Rho(double Rho){
        _Rho = Rho;
    }
    public double get_A(){
        return _A;
    }
    public double get_B(){
        return _B;
    }    
    public double get_Phi(){
        return _Phi;
    }       
    public void set_Phi(double Phi){
        _Phi = Phi;
    }
    public double get_Theta(){
        return _Theta;
    }   
    public void set_Theta(double Theta){
        _Theta = Theta;
    }
    public double get_X0(){
        return _X0;
    }    
    public double get_Y0(){
        return _Y0;
    }    
    public double get_Z0(){
        return _Z0;
    }    
    public double get_DCA(){
        return _DCA;
    }    
    public double get_Chi2(){
        return _Chi2;
    }
        
    
}