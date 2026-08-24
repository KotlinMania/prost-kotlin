import Testing
import Prost

@Suite("Prost Export Smoke Tests")
struct ProstExportTests {
    @Test("Swift module loads cleanly")
    func testSwiftModuleLoads() throws {
        #expect(true)
    }
}
