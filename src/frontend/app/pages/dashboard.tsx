import { useState, useEffect, useCallback } from "react";
import { useNavigate } from "react-router";
import { Button } from "../components/ui/button";
import { Badge } from "../components/ui/badge";
import {
  Table, TableBody, TableCell, TableHead,
  TableHeader, TableRow,
} from "../components/ui/table";
import { LogOut, Upload, FileText, Download, Trash2 } from "lucide-react";
import { UploadModal } from "../components/upload-modal";
import { JobDetailDrawer } from "../components/job-detail-drawer";
import { useAuth } from "../context/AuthContext";
import { api, FileRecord } from "../api";

type FrontendStatus = "pending" | "uploaded" | "processing" | "completed" | "failed";

const statusMap: Record<string, FrontendStatus> = {
  PENDING_UPLOAD: "pending",
  UPLOADED: "uploaded",
  PROCESSING: "processing",
  COMPLETED: "completed",
  FAILED: "failed",
};

const statusColors: Record<FrontendStatus, string> = {
  pending:    "bg-gray-500/10 text-gray-400 border-gray-500/20",
  uploaded:   "bg-blue-500/10 text-blue-400 border-blue-500/20",
  processing: "bg-yellow-500/10 text-yellow-400 border-yellow-500/20",
  completed:  "bg-green-500/10 text-green-400 border-green-500/20",
  failed:     "bg-red-500/10 text-red-400 border-red-500/20",
};

function formatBytes(bytes: number | null): string {
  if (!bytes) return "—";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return `${Math.round((bytes / Math.pow(k, i)) * 10) / 10} ${sizes[i]}`;
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString("en-US", {
    month: "short", day: "numeric", year: "numeric",
  });
}

export function Dashboard() {
  const navigate = useNavigate();
  const { email, logout, isAuthenticated } = useAuth();

  const [files, setFiles] = useState<FileRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedFile, setSelectedFile] = useState<FileRecord | null>(null);

  // Redirect if not authenticated
  useEffect(() => {
    if (!isAuthenticated) navigate("/");
  }, [isAuthenticated, navigate]);

  const fetchFiles = useCallback(async () => {
    try {
      const data = await api.files.list();
      setFiles(data);
    } catch {
      // token may have expired — api client handles redirect
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchFiles();
  }, [fetchFiles]);

  // Poll every 3 s while any file is PROCESSING
  useEffect(() => {
    const hasProcessing = files.some((f) => f.status === "PROCESSING");
    if (!hasProcessing) return;
    const id = setInterval(fetchFiles, 3000);
    return () => clearInterval(id);
  }, [files, fetchFiles]);

  // Keep drawer in sync after polling
  useEffect(() => {
    if (!selectedFile) return;
    const updated = files.find((f) => f.id === selectedFile.id);
    if (updated) setSelectedFile(updated);
  }, [files]);

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const handleUploadComplete = async (newFile: FileRecord) => {
    setIsUploadModalOpen(false);
    setFiles((prev) => [newFile, ...prev]);
    // Re-fetch to get server-authoritative state
    await fetchFiles();
  };

  const handleDelete = async (e: React.MouseEvent, fileId: string) => {
    e.stopPropagation();
    try {
      await api.files.delete(fileId);
      setFiles((prev) => prev.filter((f) => f.id !== fileId));
      if (selectedFile?.id === fileId) setSelectedFile(null);
    } catch (err: any) {
      alert(err.message);
    }
  };

  const handleDownload = async (e: React.MouseEvent, fileId: string) => {
    e.stopPropagation();
    try {
      const { downloadUrl } = await api.files.getDownloadUrl(fileId);
      window.open(downloadUrl, "_blank");
    } catch (err: any) {
      alert(err.message);
    }
  };

  return (
    <div className="min-h-screen">
      {/* Top Navigation */}
      <header className="border-b border-border bg-card">
        <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
          <h1 className="text-xl font-semibold tracking-tight">CloudPress</h1>
          <div className="flex items-center gap-4">
            <span className="text-sm text-muted-foreground">{email}</span>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleLogout}
              className="text-muted-foreground hover:text-foreground"
            >
              <LogOut className="w-4 h-4 mr-2" />
              Logout
            </Button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-6 py-8">
        <div className="mb-6 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-semibold">Files</h2>
            <p className="text-sm text-muted-foreground mt-1">
              Manage and compress your files
            </p>
          </div>
          <Button
            onClick={() => setIsUploadModalOpen(true)}
            className="bg-primary hover:bg-primary/90"
          >
            <Upload className="w-4 h-4 mr-2" />
            Upload File
          </Button>
        </div>

        {loading ? (
          <div className="text-center py-16 text-muted-foreground text-sm">
            Loading…
          </div>
        ) : files.length === 0 ? (
          <div className="border border-border rounded-lg bg-card p-12 text-center">
            <FileText className="w-12 h-12 mx-auto text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">No files yet</h3>
            <p className="text-sm text-muted-foreground mb-6">
              Upload your first file to get started
            </p>
            <Button
              onClick={() => setIsUploadModalOpen(true)}
              className="bg-primary hover:bg-primary/90"
            >
              <Upload className="w-4 h-4 mr-2" />
              Upload File
            </Button>
          </div>
        ) : (
          <div className="border border-border rounded-lg bg-card overflow-hidden">
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent border-border">
                  <TableHead className="text-muted-foreground">File Name</TableHead>
                  <TableHead className="text-muted-foreground">Size</TableHead>
                  <TableHead className="text-muted-foreground">Status</TableHead>
                  <TableHead className="text-muted-foreground">Date</TableHead>
                  <TableHead className="text-muted-foreground"></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {files.map((file) => {
                  const status = statusMap[file.status] ?? "pending";
                  return (
                    <TableRow
                      key={file.id}
                      className="border-border hover:bg-secondary/50 cursor-pointer"
                      onClick={() => setSelectedFile(file)}
                    >
                      <TableCell className="font-medium">{file.originalName}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatBytes(file.sizeBytes)}
                      </TableCell>
                      <TableCell>
                        <Badge
                          variant="outline"
                          className={`capitalize ${statusColors[status]}`}
                        >
                          {status}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {formatDate(file.createdAt)}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1">
                          <Button
                            size="sm"
                            variant="ghost"
                            disabled={file.status !== "COMPLETED"}
                            className="disabled:opacity-30"
                            onClick={(e) => handleDownload(e, file.id)}
                          >
                            <Download className="w-4 h-4 mr-1" />
                            Download
                          </Button>
                          <Button
                            size="sm"
                            variant="ghost"
                            className="text-muted-foreground hover:text-red-400"
                            onClick={(e) => handleDelete(e, file.id)}
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </div>
        )}
      </main>

      <UploadModal
        open={isUploadModalOpen}
        onOpenChange={setIsUploadModalOpen}
        onUploadComplete={handleUploadComplete}
      />

      <JobDetailDrawer
        file={selectedFile}
        open={!!selectedFile}
        onOpenChange={(open) => !open && setSelectedFile(null)}
      />
    </div>
  );
}
