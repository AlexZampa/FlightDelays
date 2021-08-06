package it.polito.tdp.extflightdelays.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jgrapht.Graphs;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import it.polito.tdp.extflightdelays.db.ExtFlightDelaysDAO;


public class Model {

	private SimpleWeightedGraph<Airport, DefaultWeightedEdge> grafo;
	private Map<Integer, Airport> idMap; 
	private ExtFlightDelaysDAO dao;
	private Map<Airport, Airport> visita;
	
	public Model() {
		this.idMap = new HashMap<Integer, Airport>();
		this.dao = new ExtFlightDelaysDAO();
		this.dao.loadAllAirports(this.idMap);
		this.visita = new HashMap<Airport, Airport>();
	}
	
	
	public void creaGrafo(int n_compagnie) {
		this.grafo = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
		
		// Aggiungo vertici
		for(Airport a: this.idMap.values()) {
			if(this.dao.getAirlinesNumber(a) > n_compagnie) {
				this.grafo.addVertex(a);
			}
			
			for(Rotta r : dao.getRotte(this.idMap)) {
				if(this.grafo.containsVertex(r.getA1()) && this.grafo.containsVertex(r.getA2())) {
					DefaultWeightedEdge edge = this.grafo.getEdge(r.getA1(), r.getA2());
					
					if(edge == null) {
						Graphs.addEdgeWithVertices(this.grafo, r.getA1(), r.getA2(), r.getPeso());
					}
					else {
						double pesoVecchio = this.grafo.getEdgeWeight(edge);
						double pesoNuovo = pesoVecchio + r.getPeso();
						this.grafo.setEdgeWeight(edge, pesoNuovo);
					}
				}
			}
			
		}	
	}
	
	
	public int vertexNumber() {
		return this.grafo.vertexSet().size();
	}
	
	public int edgeNumber() {
		return this.grafo.edgeSet().size();
	}
	
	public Collection<Airport> getAirport() {
		return this.grafo.vertexSet();
	}
	
	public List<Airport> trovaPercorso(Airport a1, Airport a2) {
		List<Airport> percorso = new ArrayList<Airport>();
		BreadthFirstIterator<Airport, DefaultWeightedEdge> iter = new BreadthFirstIterator<>(this.grafo,a1);
		// aggiungo subito radice dell'albero
		visita.put(a1, null);
		
		iter.addTraversalListener(new TraversalListener<Airport, DefaultWeightedEdge>() {
			
			@Override
			public void vertexTraversed(VertexTraversalEvent<Airport> e) {}
			
			@Override
			public void vertexFinished(VertexTraversalEvent<Airport> e) {}
			
			@Override
			public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {
				Airport sorgente = grafo.getEdgeSource(e.getEdge());
				Airport destinazione = grafo.getEdgeTarget(e.getEdge());
				
				if(!visita.containsKey(destinazione) && visita.containsKey(sorgente)) {
					 visita.put(destinazione, sorgente);
				}
				else if (!visita.containsKey(sorgente) && visita.containsKey(destinazione)) {
					visita.put(sorgente, destinazione);
				}
				
			}
			
			@Override
			public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {}
			
			@Override
			public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {}
		});
		
		
		while(iter.hasNext()) {
			iter.next();
		}
		
		if(!visita.containsKey(a1) || !visita.containsKey(a2)) {
			// i due aeroporti non sono collegati
			return null;
		}
		
		Airport step = a2;
		while(!step.equals(a1)) {
			percorso.add(step);
			step = visita.get(step);
		}
		
		percorso.add(a1);
		return percorso;
	}
	
}
