import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinIosKt.startKoinIos()   // module 7 : Koin démarre une fois, avec le module réseau iOS
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
