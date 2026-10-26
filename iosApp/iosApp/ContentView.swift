import SwiftUI
import Shared

// Module 8 : l'écran Compose partagé, présenté par SwiftUI comme n'importe quel écran
struct CallView: UIViewControllerRepresentable {
    let viewModel: CallViewModel

    func makeUIViewController(context: Context) -> UIViewController {
        CallViewControllerKt.CallViewController(viewModel: viewModel, onNavigateBack: {})
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    // Module 9 : le ViewModel vient de Koin (KoinIos.kt)
    private let viewModel = KoinIosKt.callViewModel(room: SignalingConfig.shared.ROOM)

    var body: some View {
        CallView(viewModel: viewModel)
            .ignoresSafeArea(.keyboard)
            .task {
                // Module 9, atelier 6 : avec SKIE, le StateFlow est une AsyncSequence
                for await state in viewModel.state {
                    print("Participants : \(state.participants.map { $0.id })")
                }
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
    case .chatMessageReceived(let e): return "\(e.from) : \(e.text)"   // bonus : nouveau cas, sinon le switch ne compile plus
    }
}
