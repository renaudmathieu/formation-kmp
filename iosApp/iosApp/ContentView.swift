import SwiftUI
import Shared   // module 3 : le framework Kotlin

struct ContentView: View {
    @State private var sdp = "…"

    var body: some View {
        VStack(spacing: 16) {
            Text("SDP renvoyé par Kotlin").font(.headline)
            Text(sdp).font(.footnote.monospaced())
            Text(describe(CallEventParticipantJoined(id: "alice")))
        }
        .padding()
        .task {
            // Module 3, atelier 2 : une fonction suspend Kotlin appelée en async depuis Swift
            let controller = CallController()
            sdp = (try? await controller.createOffer()) ?? "erreur"
        }
    }
}

// Module 3, atelier 4 (SKIE) : switch exhaustif sur la classe scellée, sans default.
// Mis à jour au module 4 avec les nouveaux cas, sinon « Switch must be exhaustive ».
func describe(_ event: CallEvent) -> String {
    switch onEnum(of: event) {
    case .participantJoined(let e): return "\(e.id) a rejoint l'appel"
    case .participantLeft(let e): return "\(e.id) a quitté l'appel"
    case .remoteTrackAdded(let e): return "piste distante de \(e.participantId)"
    case .callEnded: return "appel terminé"
    case .error(let e): return "erreur : \(e.message ?? "inconnue")"
    }
}
